package com.segmentanalyzer.data.remote.strava

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

@Serializable
internal data class StravaSegmentDto(
    val id: Long,
    val name: String,
    @SerialName("activity_type") val activityType: String,
    val distance: Double,
    @SerialName("average_grade") val averageGrade: Double,
    @SerialName("maximum_grade") val maximumGrade: Double,
    @SerialName("elevation_high") val elevationHigh: Double? = null,
    @SerialName("elevation_low") val elevationLow: Double? = null,
    @SerialName("climb_category") val climbCategory: Int = 0,
    val city: String? = null,
    val state: String? = null,
    @SerialName("start_latlng") val startLatLng: List<Double>? = null,
    @SerialName("end_latlng") val endLatLng: List<Double>? = null,
)

@Serializable
internal data class StravaPolylineMapDto(
    val polyline: String? = null,
    @SerialName("summary_polyline") val summaryPolyline: String? = null,
)

/**
 * `GET /segments/{id}` returns the full segment representation (same fields as
 * [StravaSegmentDto] from the starred list) plus [map] — the polyline the starred list omits.
 * Used both to backfill a starred segment's polyline and, standalone, to fetch a segment the user
 * hasn't starred/synced yet (see [com.segmentanalyzer.domain.usecase.SaveStravaSegmentEffortAttemptUseCase]).
 */
@Serializable
internal data class StravaSegmentDetailDto(
    val id: Long,
    val name: String,
    @SerialName("activity_type") val activityType: String,
    val distance: Double,
    @SerialName("average_grade") val averageGrade: Double,
    @SerialName("maximum_grade") val maximumGrade: Double,
    @SerialName("elevation_high") val elevationHigh: Double? = null,
    @SerialName("elevation_low") val elevationLow: Double? = null,
    @SerialName("climb_category") val climbCategory: Int = 0,
    val city: String? = null,
    val state: String? = null,
    @SerialName("start_latlng") val startLatLng: List<Double>? = null,
    @SerialName("end_latlng") val endLatLng: List<Double>? = null,
    val map: StravaPolylineMapDto? = null,
    val starred: Boolean = false,
)

/** Fetches the athlete's starred segments from Strava's official REST API. */
internal class StravaSegmentApi @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun fetchStarredSegments(accessToken: String): List<StravaSegmentDto> {
        val request = Request.Builder()
            .url(STARRED_SEGMENTS_URL)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        val body = try {
            okHttpClient.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw StravaApiException("HTTP ${response.code}: $text")
                text
            }
        } catch (e: IOException) {
            throw StravaApiException(e.message ?: "network error", e)
        }
        return try {
            json.decodeFromString(body)
        } catch (e: Exception) {
            throw StravaApiException("couldn't parse Strava's starred segments response", e)
        }
    }

    /** The starred-segments list has no route geometry — this fetches the full polyline for one segment. */
    fun fetchSegmentDetail(accessToken: String, segmentId: String): StravaSegmentDetailDto {
        val request = Request.Builder()
            .url("$SEGMENT_URL/$segmentId")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        val body = try {
            okHttpClient.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw StravaApiException("HTTP ${response.code}: $text")
                text
            }
        } catch (e: IOException) {
            throw StravaApiException(e.message ?: "network error", e)
        }
        return try {
            json.decodeFromString(body)
        } catch (e: Exception) {
            throw StravaApiException("couldn't parse Strava's segment detail response", e)
        }
    }

    /** Stars or unstars [segmentId] for the authenticated athlete. Requires the `profile:write` OAuth scope. */
    fun starSegment(accessToken: String, segmentId: String, starred: Boolean): StravaSegmentDetailDto {
        val request = Request.Builder()
            .url("$SEGMENT_URL/$segmentId/starred")
            .header("Authorization", "Bearer $accessToken")
            .put(FormBody.Builder().add("starred", starred.toString()).build())
            .build()
        val body = try {
            okHttpClient.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw StravaApiException("HTTP ${response.code}: $text")
                text
            }
        } catch (e: IOException) {
            throw StravaApiException(e.message ?: "network error", e)
        }
        return try {
            json.decodeFromString(body)
        } catch (e: Exception) {
            throw StravaApiException("couldn't parse Strava's star segment response", e)
        }
    }

    private companion object {
        const val STARRED_SEGMENTS_URL = "https://www.strava.com/api/v3/segments/starred"
        const val SEGMENT_URL = "https://www.strava.com/api/v3/segments"
    }
}
