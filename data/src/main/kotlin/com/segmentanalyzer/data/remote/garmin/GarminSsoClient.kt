package com.segmentanalyzer.data.remote.garmin

import okhttp3.FormBody
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
 */
internal class GarminSsoClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    suspend fun login(username: String, password: String): GarminSession {
        val csrfToken = fetchCsrfToken()
        val ticket = submitCredentials(username, password, csrfToken)
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

    private fun fetchCsrfToken(): String {
        val html = executeHtml(Request.Builder().url(signinUrl()).get().build())
        return CSRF_REGEX.find(html)?.groupValues?.get(1)
            ?: throw GarminSsoException.Unexpected("no CSRF token on the sign-in page")
    }

    private fun submitCredentials(username: String, password: String, csrfToken: String): String {
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
        val html = executeHtml(request)
        return TICKET_REGEX.find(html)?.groupValues?.get(1) ?: when {
            html.contains("verifyMFA/loginEnterMfaCode") -> throw GarminSsoException.MfaRequired
            else -> throw GarminSsoException.InvalidCredentials
        }
    }

    private fun exchangeTicketForOAuth1(ticket: String, consumer: OAuthConsumer): Pair<String, String> {
        val url = "$OAUTH_BASE_URL/preauthorized"
        val params = linkedMapOf(
            "ticket" to ticket,
            "login-url" to "$SSO_BASE_URL/embed",
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
            .post("".toRequestBody(null))
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

    private fun executeApi(request: Request): String = try {
        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw GarminSsoException.Unexpected("HTTP ${response.code}")
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

    private companion object {
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
        fun queryFieldRegex(field: String) = Regex("""(?:^|&)$field=([^&]+)""")
        fun jsonStringField(field: String) = Regex(""""$field"\s*:\s*"([^"]+)"""")
        fun jsonNumberField(field: String) = Regex(""""$field"\s*:\s*(\d+)""")
    }
}
