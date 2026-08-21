package com.segmentanalyzer.data.remote.strava

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

@Serializable
internal data class StravaActivitySummaryDto(
    val id: Long,
    @SerialName("start_date") val startDate: String,
)

@Serializable
internal data class StravaSegmentEffortSegmentRefDto(
    val id: Long,
)

@Serializable
internal data class StravaSegmentEffortDto(
    val id: Long,
    val name: String,
    @SerialName("elapsed_time") val elapsedTime: Int,
    val distance: Double,
    @SerialName("kom_rank") val komRank: Int? = null,
    @SerialName("pr_rank") val prRank: Int? = null,
    val segment: StravaSegmentEffortSegmentRefDto? = null,
)

@Serializable
internal data class StravaActivityDetailDto(
    @SerialName("segment_efforts") val segmentEfforts: List<StravaSegmentEffortDto> = emptyList(),
)

/** One requested stream (e.g. "watts", "heartrate") from a segment effort's `/streams` response. */
@Serializable
internal data class StravaStreamDto(
    val type: String,
    val data: List<Double> = emptyList(),
)

/**
 * Fetches the athlete's activities and per-activity segment effort data from Strava's official
 * REST API. Requires the `activity:read_all` OAuth scope — the narrower `read` scope this app
 * originally requested only covers public profile/segment data, not activities. `activity:read`
 * alone was tried first but left `GET /segments/{id}/all_efforts` returning an empty list.
 */
internal class StravaActivityApi @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Activities starting within [afterEpochSeconds, beforeEpochSeconds]. */
    fun fetchActivities(
        accessToken: String,
        afterEpochSeconds: Long,
        beforeEpochSeconds: Long,
    ): List<StravaActivitySummaryDto> = get(
        "$ATHLETE_ACTIVITIES_URL?after=$afterEpochSeconds&before=$beforeEpochSeconds&per_page=30",
        accessToken,
        "activity list",
    )

    fun fetchActivityDetail(accessToken: String, activityId: Long): StravaActivityDetailDto =
        get("$ACTIVITY_URL/$activityId", accessToken, "activity detail")

    /**
     * Point-by-point streams for one specific segment effort (identified by its own id, not the
     * segment's — see [StravaSegmentEffortDto.id]). `latlng` is deliberately not requested since
     * nothing here renders a map yet; the rest is enough for pace/power/HR/cadence summaries.
     */
    fun fetchEffortStreams(accessToken: String, effortId: Long): List<StravaStreamDto> = get(
        "$SEGMENT_EFFORT_URL/$effortId/streams?keys=$STREAM_KEYS",
        accessToken,
        "segment effort streams",
    )

    private inline fun <reified T> get(url: String, accessToken: String, what: String): T {
        val request = Request.Builder()
            .url(url)
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
            throw StravaApiException("couldn't parse Strava's $what response", e)
        }
    }

    private companion object {
        /** The athlete-scoped activity list — NOT the same base path as the single-activity detail endpoint below. */
        const val ATHLETE_ACTIVITIES_URL = "https://www.strava.com/api/v3/athlete/activities"
        const val ACTIVITY_URL = "https://www.strava.com/api/v3/activities"
        const val SEGMENT_EFFORT_URL = "https://www.strava.com/api/v3/segment_efforts"
        const val STREAM_KEYS = "time,distance,altitude,velocity_smooth,heartrate,cadence,watts,grade_smooth"
    }
}
