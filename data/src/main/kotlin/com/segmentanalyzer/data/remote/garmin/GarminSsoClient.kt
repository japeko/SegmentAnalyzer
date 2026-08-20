package com.segmentanalyzer.data.remote.garmin

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

/**
 * Completes Garmin Connect's unofficial SSO flow: given the CAS-style "ticket" produced once a
 * rider finishes signing in (in a WebView showing Garmin's real page — see [signinUrl] and
 * [ticketFrom]), exchange it for OAuth1 credentials, then OAuth2 tokens. Mirrors the
 * reverse-engineered flow used by the garth / python-garminconnect open-source clients, since
 * Garmin has no public OAuth API for third-party apps — endpoint shapes here are undocumented and
 * Garmin can change them without notice.
 *
 * Sign-in itself is deliberately *not* done via raw HTTP requests here: Garmin's `/sso/signin`
 * endpoint is now behind Cloudflare's bot-detection JS challenge (confirmed live — the response
 * embeds a `/cdn-cgi/challenge-platform/...` script), which only a real browser engine can satisfy.
 * A WebView is a real browser engine; a plain OkHttp POST is not.
 */
internal class GarminSsoClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    /** The URL to load in a WebView for the rider to sign in on Garmin's real page. */
    fun signinUrl(): String = "$SSO_BASE_URL/signin?" + encodeQuery(SIGNIN_PARAMS)

    /** True once [url] (observed as the WebView navigates) carries a completed-login ticket. */
    fun ticketFrom(url: String): String? = TICKET_REGEX.find(url)?.groupValues?.get(1)

    /**
     * The service a ticket in [url] was actually issued for — [url] itself minus the `ticket`
     * query param. Confirmed live that this is *not* always `SIGNIN_PARAMS["service"]`: Garmin's
     * own client-side JS sometimes redirects `/modern` → `/app` and re-issues the ticket against
     * `/app` instead, and Garmin 401s a `preauthorized` call whose `login-url` doesn't exactly
     * match whatever service the ticket was really issued for.
     */
    fun serviceFrom(url: String): String? {
        val httpUrl = url.toHttpUrlOrNull() ?: return null
        if (httpUrl.queryParameter("ticket") == null) return null
        return httpUrl.newBuilder().removeAllQueryParameters("ticket").build().toString()
    }

    suspend fun completeSession(username: String, ticket: String, service: String): GarminSession {
        val consumer = fetchOAuthConsumer()
        val (oauth1Token, oauth1Secret) = exchangeTicketForOAuth1(ticket, service, consumer)
        val (accessToken, refreshToken, expiresInSeconds) =
            exchangeOAuth1ForOAuth2(oauth1Token, oauth1Secret, consumer)
        return GarminSession(
            username = username,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = Instant.now().plusSeconds(expiresInSeconds),
        )
    }

    private fun exchangeTicketForOAuth1(ticket: String, service: String, consumer: OAuthConsumer): Pair<String, String> {
        val url = "$OAUTH_BASE_URL/preauthorized"
        val params = linkedMapOf(
            // Must match the service the ticket was actually issued for — Garmin validates the
            // two match and 401s otherwise, per its own (helpfully explicit) error.
            "ticket" to ticket,
            "login-url" to service,
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

    private fun encodeQuery(params: Map<String, String>): String =
        params.entries.joinToString("&") { (key, value) ->
            "${GarminOAuth1Signer.percentEncode(key)}=${GarminOAuth1Signer.percentEncode(value)}"
        }

    private data class OAuthConsumer(val key: String, val secret: String)

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

        val TICKET_REGEX = Regex("""ticket=([^"'&]+)""")
        fun queryFieldRegex(field: String) = Regex("""(?:^|&)$field=([^&]+)""")
        fun jsonStringField(field: String) = Regex(""""$field"\s*:\s*"([^"]+)"""")
        fun jsonNumberField(field: String) = Regex(""""$field"\s*:\s*(\d+)""")
    }
}
