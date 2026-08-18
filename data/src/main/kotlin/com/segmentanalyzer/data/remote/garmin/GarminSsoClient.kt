package com.segmentanalyzer.data.remote.garmin

import android.util.Log
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

/**
 * Logs in to Garmin Connect via its unofficial SSO web flow: CSRF-protected form login, then
 * exchange the resulting service ticket for OAuth1 credentials, then exchange those for an
 * OAuth2 bearer token. Mirrors the reverse-engineered flow used by the garth / python-garminconnect
 * open-source clients, since Garmin has no public OAuth API for third-party apps — endpoint shapes
 * here are undocumented and Garmin can change them without notice.
 *
 * Accounts with multi-factor auth enabled get an extra round trip: [login] returns
 * [GarminSsoStep.MfaRequired] instead of throwing, and the in-progress login (its MFA-page CSRF
 * token) is held here until [submitMfaCode] completes it. This relies on the same [OkHttpClient]
 * (and its cookie jar) being reused between the two calls, which holds since this class is only
 * ever constructed once, injected into the singleton `GarminAccountRepositoryImpl`.
 */
internal class GarminSsoClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private var pendingMfa: PendingMfa? = null

    // Garmin's SSO is a classic CAS-style flow: success/MFA/failure is signaled by a redirect's
    // Location header (e.g. a "...?ticket=..." target), not by the body of whatever page that
    // redirect ultimately lands on. OkHttp follows redirects by default, which was silently
    // throwing that signal away and leaving us looking at a generic landing-page shell instead.
    private val noRedirectClient by lazy {
        okHttpClient.newBuilder().followRedirects(false).followSslRedirects(false).build()
    }

    suspend fun login(username: String, password: String): GarminSsoStep {
        seedEmbedCookies()
        val csrfToken = fetchCsrfToken()
        return when (val result = submitCredentials(username, password, csrfToken)) {
            is CredentialsResult.Ticket -> GarminSsoStep.LoggedIn(completeSession(username, result.ticket))
            is CredentialsResult.MfaRequired -> {
                pendingMfa = PendingMfa(username, result.csrfToken, result.pageUrl)
                GarminSsoStep.MfaRequired
            }
        }
    }

    suspend fun submitMfaCode(code: String): GarminSsoStep.LoggedIn {
        val pending = pendingMfa ?: throw GarminSsoException.Unexpected("no Garmin login is in progress")
        Log.d(TAG, "submitting MFA code to ${pending.pageUrl} with csrf=${pending.csrfToken.take(8)}...")
        val body = FormBody.Builder()
            .add("mfa-code", code)
            .add("embed", "false")
            .add("_csrf", pending.csrfToken)
            .add("fromPage", "setupEnterMfaCode")
            .build()
        val request = Request.Builder()
            .url(pending.pageUrl)
            .header("Referer", pending.pageUrl)
            .post(body)
            .build()
        val response = executeNoRedirect(request)
        val ticket = resolveTicket(response) ?: run {
            Log.d(
                TAG,
                "MFA code not accepted: HTTP ${response.code}, Location=${response.location}, " +
                    "body starts with: ${response.body.take(1500)}",
            )
            if (response.code == 429) throw GarminSsoException.Unexpected(RATE_LIMITED_MESSAGE)
            throw GarminSsoException.Unexpected(
                "that code wasn't accepted — check it and try again (see logcat tag \"$TAG\")",
            )
        }
        pendingMfa = null
        return GarminSsoStep.LoggedIn(completeSession(pending.username, ticket))
    }

    private fun completeSession(username: String, ticket: String): GarminSession {
        val consumer = fetchOAuthConsumer()
        val (oauth1Token, oauth1Secret) = exchangeTicketForOAuth1(ticket, consumer)
        val (accessToken, refreshToken, expiresInSeconds) =
            exchangeOAuth1ForOAuth2(oauth1Token, oauth1Secret, consumer)
        return GarminSession(
            username = username,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = Instant.now().plusSeconds(expiresInSeconds),
        )
    }

    /**
     * Garmin's embed widget expects to be primed with cookies from `/sso/embed` before `/sso/signin`
     * is ever touched. Skipping this (as this client did until now) still lets the sign-in page and
     * CSRF token load fine, but the session apparently isn't fully valid — Garmin accepts the
     * credentials POST and even the MFA redirect, but the MFA code POST silently bounces back to a
     * fresh sign-in instead of completing, which matches losing track of an incompletely-seeded flow.
     */
    private fun seedEmbedCookies() {
        val params = linkedMapOf("id" to "gauth-widget", "embedWidget" to "true")
        val url = "$SSO_BASE_URL/embed?" + encodeQuery(params)
        executeHtml(Request.Builder().url(url).get().build())
    }

    private fun fetchCsrfToken(): String {
        val html = executeHtml(Request.Builder().url(signinUrl()).get().build())
        return CSRF_REGEX.find(html)?.groupValues?.get(1)
            ?: throw GarminSsoException.Unexpected("no CSRF token on the sign-in page")
    }

    private fun submitCredentials(username: String, password: String, csrfToken: String): CredentialsResult {
        val body = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .add("embed", "false")
            .add("_csrf", csrfToken)
            .build()
        val request = Request.Builder()
            .url(signinUrl())
            .header("Referer", signinUrl())
            .post(body)
            .build()
        val response = executeNoRedirect(request)

        resolveTicket(response)?.let { return CredentialsResult.Ticket(it) }

        if (response.location?.let { MFA_MARKER_REGEX.containsMatchIn(it) } == true) {
            val mfaPageUrl = resolve(response.location)
            val mfaPageHtml = executeHtml(Request.Builder().url(mfaPageUrl).get().build())
            logForms(mfaPageHtml)
            val mfaCsrfToken = CSRF_REGEX.find(mfaPageHtml)?.groupValues?.get(1)
                ?: throw GarminSsoException.Unexpected("no CSRF token on the MFA page")
            return CredentialsResult.MfaRequired(mfaCsrfToken, mfaPageUrl)
        }
        if (MFA_MARKER_REGEX.containsMatchIn(response.body)) {
            val mfaCsrfToken = CSRF_REGEX.find(response.body)?.groupValues?.get(1)
                ?: throw GarminSsoException.Unexpected("no CSRF token on the MFA page")
            return CredentialsResult.MfaRequired(mfaCsrfToken, signinUrl())
        }

        Log.d(
            TAG,
            "unrecognized sign-in response: HTTP ${response.code}, Location=${response.location}, " +
                "body starts with: ${response.body.take(1500)}",
        )

        if (response.code == 429) throw GarminSsoException.Unexpected(RATE_LIMITED_MESSAGE)

        // Genuinely wrong credentials land back on a page titled "Sign In" with no ticket and no
        // MFA marker. Anything else unrecognized (Garmin changed the page, a captcha, a locked
        // account, ...) shouldn't be silently mislabeled as bad credentials — surface what Garmin
        // actually returned instead so it's fixable.
        val title = TITLE_REGEX.find(response.body)?.groupValues?.get(1)?.trim()
        if (response.code !in 300..399 && (title == null || title.equals("Sign In", ignoreCase = true))) {
            throw GarminSsoException.InvalidCredentials
        }
        throw GarminSsoException.Unexpected(
            "unrecognized response from Garmin (HTTP ${response.code}" +
                (response.location?.let { ", redirected to: $it" } ?: "") +
                (title?.let { ", page title: \"$it\"" } ?: "") +
                ") — see logcat tag \"$TAG\" for the full response",
        )
    }

    /** Logs every `<form>` tag and `<input>` field name/value found, chunked to survive logcat's per-entry size limit. */
    private fun logForms(html: String) {
        val forms = FORM_TAG_REGEX.findAll(html).map { it.value }.joinToString("\n")
        val inputs = INPUT_TAG_REGEX.findAll(html).map { it.value }.joinToString("\n")
        val combined = "forms:\n$forms\n\ninputs:\n$inputs"
        combined.chunked(1000).forEachIndexed { index, chunk ->
            Log.d(TAG, "page form fields [$index]: $chunk")
        }
    }

    /** A ticket can show up either in a redirect's target URL or embedded in a 200 page's body. */
    private fun ticketFrom(response: RawResponse): String? =
        response.location?.let { TICKET_REGEX.find(it)?.groupValues?.get(1) }
            ?: TICKET_REGEX.find(response.body)?.groupValues?.get(1)

    /**
     * The ticket can be more than one redirect away — e.g. a successful MFA code POST redirects
     * to `/sso/login?logintoken=...`, which itself redirects again to the final ticket URL. Follow
     * up to [maxHops] further redirects (as plain GETs) looking for a ticket at each step.
     */
    private fun resolveTicket(initial: RawResponse, maxHops: Int = 5): String? {
        var response = initial
        repeat(maxHops + 1) {
            ticketFrom(response)?.let { return it }
            val location = response.location ?: return null
            response = executeNoRedirect(Request.Builder().url(resolve(location)).get().build())
        }
        return null
    }

    private fun resolve(location: String): String =
        if (location.startsWith("http")) {
            location
        } else {
            signinUrl().toHttpUrl().resolve(location)?.toString() ?: location
        }

    private fun exchangeTicketForOAuth1(ticket: String, consumer: OAuthConsumer): Pair<String, String> {
        val url = "$OAUTH_BASE_URL/preauthorized"
        val params = linkedMapOf(
            // Must match the "service" a ticket was actually issued for (SIGNIN_PARAMS) — Garmin
            // validates the two match and 401s otherwise, per its own (helpfully explicit) error.
            "ticket" to ticket,
            "login-url" to SIGNIN_PARAMS.getValue("service"),
            "accepts-mfa-tokens" to "true",
        )
        val authHeader = GarminOAuth1Signer.authorizationHeader(
            method = "GET",
            url = url,
            consumerKey = consumer.key,
            consumerSecret = consumer.secret,
            extraParams = params,
        )
        val request = Request.Builder()
            .url(url + "?" + encodeQuery(params))
            .header("Authorization", authHeader)
            .get()
            .build()
        val body = executeApi(request)
        val token = queryFieldRegex("oauth_token").find(body)?.groupValues?.get(1)
        val secret = queryFieldRegex("oauth_token_secret").find(body)?.groupValues?.get(1)
        return token?.let { t -> secret?.let { s -> t to s } }
            ?: throw GarminSsoException.Unexpected("no OAuth1 token in preauthorized response")
    }

    private fun exchangeOAuth1ForOAuth2(
        oauth1Token: String,
        oauth1Secret: String,
        consumer: OAuthConsumer,
    ): Triple<String, String, Long> {
        val url = "$OAUTH_BASE_URL/exchange/user/2.0"
        val authHeader = GarminOAuth1Signer.authorizationHeader(
            method = "POST",
            url = url,
            consumerKey = consumer.key,
            consumerSecret = consumer.secret,
            token = oauth1Token,
            tokenSecret = oauth1Secret,
        )
        val request = Request.Builder()
            .url(url)
            .header("Authorization", authHeader)
            .post("".toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()
        val json = executeApi(request)
        val accessToken = jsonStringField("access_token").find(json)?.groupValues?.get(1)
        val refreshToken = jsonStringField("refresh_token").find(json)?.groupValues?.get(1)
        val expiresIn = jsonNumberField("expires_in").find(json)?.groupValues?.get(1)?.toLongOrNull()
        if (accessToken == null || refreshToken == null || expiresIn == null) {
            throw GarminSsoException.Unexpected("no OAuth2 token in exchange response")
        }
        return Triple(accessToken, refreshToken, expiresIn)
    }

    /** Garmin's public consumer key/secret, resolved at runtime (not hardcoded) since it can rotate. */
    private fun fetchOAuthConsumer(): OAuthConsumer {
        val json = executeApi(Request.Builder().url(OAUTH_CONSUMER_URL).get().build())
        val key = jsonStringField("consumer_key").find(json)?.groupValues?.get(1)
        val secret = jsonStringField("consumer_secret").find(json)?.groupValues?.get(1)
        return key?.let { k -> secret?.let { s -> OAuthConsumer(k, s) } }
            ?: throw GarminSsoException.Unexpected("couldn't resolve Garmin's OAuth1 consumer credentials")
    }

    /** SSO web pages always return 200, success/failure is distinguished by page content. */
    private fun executeHtml(request: Request): String = try {
        okHttpClient.newCall(request).execute().use { it.body?.string().orEmpty() }
    } catch (e: IOException) {
        throw GarminSsoException.Unavailable(e.message ?: "network error")
    }

    /** Like [executeHtml] but doesn't follow redirects, so the Location header stays inspectable. */
    private fun executeNoRedirect(request: Request): RawResponse = try {
        noRedirectClient.newCall(request).execute().use { response ->
            RawResponse(
                code = response.code,
                location = response.header("Location"),
                body = response.body?.string().orEmpty(),
            )
        }
    } catch (e: IOException) {
        throw GarminSsoException.Unavailable(e.message ?: "network error")
    }

    private fun executeApi(request: Request): String = try {
        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.d(TAG, "API call failed: ${request.method} ${request.url} -> HTTP ${response.code}: $body")
                if (response.code == 429) throw GarminSsoException.Unexpected(RATE_LIMITED_MESSAGE)
                throw GarminSsoException.Unexpected("HTTP ${response.code}")
            }
            body
        }
    } catch (e: IOException) {
        throw GarminSsoException.Unavailable(e.message ?: "network error")
    }

    private fun signinUrl(): String = "$SSO_BASE_URL/signin?" + encodeQuery(SIGNIN_PARAMS)

    private fun encodeQuery(params: Map<String, String>): String =
        params.entries.joinToString("&") { (key, value) ->
            "${GarminOAuth1Signer.percentEncode(key)}=${GarminOAuth1Signer.percentEncode(value)}"
        }

    private data class OAuthConsumer(val key: String, val secret: String)
    private data class PendingMfa(val username: String, val csrfToken: String, val pageUrl: String)
    private data class RawResponse(val code: Int, val location: String?, val body: String)

    private sealed class CredentialsResult {
        data class Ticket(val ticket: String) : CredentialsResult()
        data class MfaRequired(val csrfToken: String, val pageUrl: String) : CredentialsResult()
    }

    private companion object {
        const val TAG = "GarminSso"
        const val RATE_LIMITED_MESSAGE = "Garmin is temporarily rate-limiting this device — wait a few minutes and try again"
        const val SSO_BASE_URL = "https://sso.garmin.com/sso"
        const val OAUTH_BASE_URL = "https://connectapi.garmin.com/oauth-service/oauth"
        const val OAUTH_CONSUMER_URL = "https://thegarth.s3.amazonaws.com/oauth_consumer.json"

        val SIGNIN_PARAMS = linkedMapOf(
            "id" to "gauth-widget",
            "embedWidget" to "false",
            "gauthHost" to SSO_BASE_URL,
            "service" to "https://connect.garmin.com/modern",
            "source" to "https://connect.garmin.com/signin",
            "redirectAfterAccountLoginUrl" to "https://connect.garmin.com/modern",
            "redirectAfterAccountCreationUrl" to "https://connect.garmin.com/modern",
            "clientId" to "GarminConnect",
            "consumeServiceTicket" to "false",
            "generateExtraServiceTicket" to "true",
            "generateTwoExtraServiceTickets" to "true",
            "generateNoServiceTicket" to "false",
            "globalOptInShown" to "false",
            "globalOptInChecked" to "false",
            "mobile" to "false",
            "connectLegalTerms" to "true",
            "locale" to "en_US",
            "showTermsOfUse" to "false",
            "showPrivacyPolicy" to "false",
            "showConnectLegalAge" to "false",
            "locationPromptShown" to "true",
            "showPassword" to "true",
            "useCustomHeader" to "false",
            "mfaRequired" to "false",
            "performMFACheck" to "false",
            "rememberMyBrowserShown" to "false",
            "rememberMyBrowserChecked" to "false",
            "rememberMeShown" to "true",
            "rememberMeChecked" to "false",
            "createAccountShown" to "true",
            "openCreateAccount" to "false",
            "displayNameShown" to "false",
            "initialFocus" to "true",
        )

        val CSRF_REGEX = Regex("""name="_csrf"\s+value="([^"]+)"""")
        val TICKET_REGEX = Regex("""ticket=([^"'&]+)""")
        val TITLE_REGEX = Regex("""<title>(.*?)</title>""", RegexOption.IGNORE_CASE)
        val FORM_TAG_REGEX = Regex("""<form\b[^>]*>""", RegexOption.IGNORE_CASE)
        val INPUT_TAG_REGEX = Regex("""<input\b[^>]*>""", RegexOption.IGNORE_CASE)

        // Broad on purpose: Garmin's exact MFA marker/URL is unverified against a live account,
        // so this matches on any of several plausible signals rather than one exact string.
        val MFA_MARKER_REGEX = Regex(
            """verifyMFA|loginEnterMfaCode|mfa-code|two-factor|verification code""",
            RegexOption.IGNORE_CASE,
        )
        fun queryFieldRegex(field: String) = Regex("""(?:^|&)$field=([^&]+)""")
        fun jsonStringField(field: String) = Regex(""""$field"\s*:\s*"([^"]+)"""")
        fun jsonNumberField(field: String) = Regex(""""$field"\s*:\s*(\d+)""")
    }
}
