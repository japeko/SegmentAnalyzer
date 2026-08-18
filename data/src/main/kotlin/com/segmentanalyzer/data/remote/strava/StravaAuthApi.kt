package com.segmentanalyzer.data.remote.strava

import com.segmentanalyzer.data.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import javax.inject.Inject

@Serializable
internal data class StravaAthleteDto(
    val firstname: String? = null,
    val lastname: String? = null,
)

@Serializable
internal data class StravaTokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_at") val expiresAt: Long,
    val athlete: StravaAthleteDto? = null,
)

/** Strava's official OAuth2 token endpoint — authorization-code exchange and refresh. */
internal class StravaAuthApi @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun exchangeCode(code: String): StravaTokenResponseDto = post(
        FormBody.Builder()
            .add("client_id", BuildConfig.STRAVA_CLIENT_ID)
            .add("client_secret", BuildConfig.STRAVA_CLIENT_SECRET)
            .add("code", code)
            .add("grant_type", "authorization_code")
            .build(),
    )

    /** Refresh responses don't include the athlete object, only the initial code exchange does. */
    fun refreshToken(refreshToken: String): StravaTokenResponseDto = post(
        FormBody.Builder()
            .add("client_id", BuildConfig.STRAVA_CLIENT_ID)
            .add("client_secret", BuildConfig.STRAVA_CLIENT_SECRET)
            .add("refresh_token", refreshToken)
            .add("grant_type", "refresh_token")
            .build(),
    )

    private fun post(body: RequestBody): StravaTokenResponseDto {
        val request = Request.Builder().url(TOKEN_URL).post(body).build()
        val responseBody = try {
            okHttpClient.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw StravaApiException("HTTP ${response.code}: $text")
                text
            }
        } catch (e: IOException) {
            throw StravaApiException(e.message ?: "network error", e)
        }
        return try {
            json.decodeFromString(responseBody)
        } catch (e: Exception) {
            throw StravaApiException("couldn't parse Strava's token response", e)
        }
    }

    private companion object {
        const val TOKEN_URL = "https://www.strava.com/oauth/token"
    }
}
