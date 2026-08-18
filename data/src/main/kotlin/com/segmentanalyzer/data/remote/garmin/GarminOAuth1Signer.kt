package com.segmentanalyzer.data.remote.garmin

import java.net.URLEncoder
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Minimal RFC 5849 OAuth1 request signer, just enough for Garmin's oauth-service endpoints. */
internal object GarminOAuth1Signer {

    fun authorizationHeader(
        method: String,
        url: String,
        consumerKey: String,
        consumerSecret: String,
        token: String? = null,
        tokenSecret: String? = null,
        extraParams: Map<String, String> = emptyMap(),
    ): String {
        val oauthParams = linkedMapOf(
            "oauth_consumer_key" to consumerKey,
            "oauth_nonce" to UUID.randomUUID().toString().replace("-", ""),
            "oauth_signature_method" to "HMAC-SHA1",
            "oauth_timestamp" to (System.currentTimeMillis() / 1000).toString(),
            "oauth_version" to "1.0",
        )
        if (token != null) oauthParams["oauth_token"] = token

        val signatureBaseString = signatureBaseString(method, url, oauthParams + extraParams)
        val signingKey = "${percentEncode(consumerSecret)}&${percentEncode(tokenSecret.orEmpty())}"
        val signature = hmacSha1(signingKey, signatureBaseString)

        val headerParams = oauthParams + ("oauth_signature" to signature)
        return "OAuth " + headerParams.entries.joinToString(", ") { (key, value) ->
            "${percentEncode(key)}=\"${percentEncode(value)}\""
        }
    }

    private fun signatureBaseString(method: String, url: String, params: Map<String, String>): String {
        val baseUrl = url.substringBefore("?")
        val normalizedParams = params.toSortedMap().entries.joinToString("&") { (key, value) ->
            "${percentEncode(key)}=${percentEncode(value)}"
        }
        return listOf(method.uppercase(), percentEncode(baseUrl), percentEncode(normalizedParams))
            .joinToString("&")
    }

    private fun hmacSha1(key: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        return Base64.getEncoder().encodeToString(mac.doFinal(data.toByteArray(Charsets.UTF_8)))
    }

    /** RFC 3986 unreserved-character percent-encoding (stricter than [URLEncoder]'s form-encoding). */
    fun percentEncode(value: String): String =
        URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
}
