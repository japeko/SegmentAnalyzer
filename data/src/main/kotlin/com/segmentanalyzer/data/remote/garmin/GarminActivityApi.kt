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

@Serializable
internal data class GarminMetricDescriptorDto(val metricsIndex: Int, val key: String)

@Serializable
internal data class GarminActivityDetailMetricDto(val metrics: List<Double?>)

@Serializable
internal data class GarminActivityDetailsDto(
    val metricDescriptors: List<GarminMetricDescriptorDto>,
    val activityDetailMetrics: List<GarminActivityDetailMetricDto>,
)

/** Thrown for any failure talking to Garmin Connect's authenticated API. */
internal class GarminApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Fetches the rider's activities, and one activity's full GPS track, from Garmin Connect's
 * authenticated REST API. Undocumented/reverse-engineered, like [GarminSsoClient] — field names
 * are best-effort and may need adjusting once verified against a live account (these endpoints
 * return many more fields than the DTOs here declare; `ignoreUnknownKeys` means that's fine).
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
            append(ACTIVITIES_URL).append("?start=0&limit=$limit")
            // LocalDate.toString() is ISO_LOCAL_DATE (yyyy-MM-dd), the format this endpoint expects.
            startDate?.let { append("&startDate=$it") }
            endDate?.let { append("&endDate=$it") }
        }
        return execute(url, accessToken, "activity list")
    }

    /**
     * One activity's full-resolution GPS track. `maxChartSize`/`maxPolylineSize` are set high
     * enough (confirmed live) to make Garmin return every recorded sample instead of a
     * downsampled subset — without them this endpoint silently thins long rides to ~250-300 points.
     */
    fun fetchActivityDetails(accessToken: String, activityId: Long): GarminActivityDetailsDto {
        val url = "$DETAILS_URL/$activityId/details?maxChartSize=$MAX_TRACK_POINTS&maxPolylineSize=$MAX_TRACK_POINTS"
        return execute(url, accessToken, "activity details")
    }

    private inline fun <reified T> execute(url: String, accessToken: String, what: String): T {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        val body = try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw GarminApiException("HTTP ${response.code} fetching $what")
                }
                response.body?.string().orEmpty()
            }
        } catch (e: IOException) {
            throw GarminApiException(e.message ?: "network error", e)
        }
        return try {
            json.decodeFromString(body)
        } catch (e: Exception) {
            throw GarminApiException("couldn't parse Garmin's $what response", e)
        }
    }

    private companion object {
        const val ACTIVITIES_URL = "https://connectapi.garmin.com/activitylist-service/activities/search/activities"
        const val DETAILS_URL = "https://connectapi.garmin.com/activity-service/activity"
        const val MAX_TRACK_POINTS = 999_999
    }
}
