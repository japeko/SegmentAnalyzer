package com.segmentanalyzer.data.remote.garmin

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

@Serializable
internal data class GarminActivityTypeDto(val typeKey: String)

@Serializable
internal data class GarminActivityDto(
    val activityId: Long,
    val activityName: String,
    val startTimeLocal: String,
    val activityType: GarminActivityTypeDto,
    val distance: Double? = null,
    val duration: Double? = null,
    val elevationGain: Double? = null,
)

/** Thrown for any failure talking to Garmin Connect's authenticated API. */
internal class GarminApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Fetches the rider's recent activities from Garmin Connect's authenticated REST API.
 * Undocumented/reverse-engineered, like [GarminSsoClient] — field names are best-effort and may
 * need adjusting once verified against a live account (this endpoint returns many more fields
 * than [GarminActivityDto] declares; `ignoreUnknownKeys` means that's fine).
 */
internal class GarminActivityApi @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun fetchActivities(
        accessToken: String,
        limit: Int,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
    ): List<GarminActivityDto> {
        val url = buildString {
            append(BASE_URL).append("?start=0&limit=$limit")
            // LocalDate.toString() is ISO_LOCAL_DATE (yyyy-MM-dd), the format this endpoint expects.
            startDate?.let { append("&startDate=$it") }
            endDate?.let { append("&endDate=$it") }
        }
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        val body = try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw GarminApiException("HTTP ${response.code} fetching activities")
                }
                response.body?.string().orEmpty()
            }
        } catch (e: IOException) {
            throw GarminApiException(e.message ?: "network error", e)
        }
        return try {
            json.decodeFromString(body)
        } catch (e: Exception) {
            throw GarminApiException("couldn't parse Garmin's activity list response", e)
        }
    }

    private companion object {
        const val BASE_URL = "https://connectapi.garmin.com/activitylist-service/activities/search/activities"
    }
}
