package com.segmentanalyzer.data.repository

import com.segmentanalyzer.common.DispatcherProvider
import com.segmentanalyzer.data.local.StravaSessionStore
import com.segmentanalyzer.data.remote.strava.StravaActivityApi
import com.segmentanalyzer.data.remote.strava.StravaActivitySummaryDto
import com.segmentanalyzer.data.remote.strava.StravaAuthApi
import com.segmentanalyzer.data.remote.strava.StravaSegmentEffortDto
import com.segmentanalyzer.data.remote.strava.StravaStreamDto
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.model.StravaSegmentEffortDetail
import com.segmentanalyzer.domain.repository.StravaActivityRepository
import com.segmentanalyzer.domain.repository.StravaSessionExpiredException
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

internal class StravaActivityRepositoryImpl @Inject constructor(
    private val activityApi: StravaActivityApi,
    private val authApi: StravaAuthApi,
    private val sessionStore: StravaSessionStore,
    private val dispatcherProvider: DispatcherProvider,
) : StravaActivityRepository {

    override suspend fun fetchSegmentEfforts(ride: Ride): Result<List<StravaSegmentEffort>> =
        withContext(dispatcherProvider.io) {
            runCatching {
                val session = validStravaSession(sessionStore, authApi) ?: throw StravaSessionExpiredException()

                val candidates = activityApi.fetchActivities(
                    accessToken = session.accessToken,
                    afterEpochSeconds = ride.startTime.minus(MATCH_TOLERANCE).epochSecond,
                    beforeEpochSeconds = ride.startTime.plus(MATCH_TOLERANCE).epochSecond,
                )
                val activityId = candidates.closestTo(ride.startTime)?.id ?: return@runCatching emptyList()

                activityApi.fetchActivityDetail(session.accessToken, activityId).segmentEfforts.map { it.toDomain() }
            }
        }

    override suspend fun fetchEffortDetail(effortExternalId: String): Result<StravaSegmentEffortDetail> =
        withContext(dispatcherProvider.io) {
            runCatching {
                val session = validStravaSession(sessionStore, authApi) ?: throw StravaSessionExpiredException()
                val streams = activityApi.fetchEffortStreams(session.accessToken, effortExternalId.toLong())
                streams.toDetail()
            }
        }

    private companion object {
        /** How far Strava's activity start time may drift from the ride's and still count as a match. */
        val MATCH_TOLERANCE: Duration = Duration.ofMinutes(5)
    }
}

/**
 * Strava has no cross-source id to match a ride against, so this finds whichever candidate
 * activity's start time is nearest [target] — the list is already narrowed to a tight window by
 * the `after`/`before` query, so ties are effectively impossible in practice.
 */
internal fun List<StravaActivitySummaryDto>.closestTo(target: Instant): StravaActivitySummaryDto? =
    minByOrNull { Duration.between(it.startDate(), target).abs() }

private fun StravaActivitySummaryDto.startDate(): Instant = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(startDate))

private fun StravaSegmentEffortDto.toDomain(): StravaSegmentEffort = StravaSegmentEffort(
    effortExternalId = id.toString(),
    segmentExternalId = segment?.id?.toString().orEmpty(),
    segmentName = name,
    elapsedTime = Duration.ofSeconds(elapsedTime.toLong()),
    distanceMeters = distance,
    komRank = komRank,
    prRank = prRank,
)

/**
 * Reduces raw point-by-point streams to summary stats. Sensor streams (watts/heartrate/cadence)
 * are simply absent from Strava's response when the ride had no reading for them, so those
 * summaries stay null rather than averaging in phantom zeros.
 */
private fun List<StravaStreamDto>.toDetail(): StravaSegmentEffortDetail {
    fun streamOrNull(type: String): List<Double>? = find { it.type == type }?.data?.takeIf { it.isNotEmpty() }

    val velocitySmooth = streamOrNull("velocity_smooth").orEmpty()
    val altitude = streamOrNull("altitude").orEmpty()
    val elevationGain = altitude.zipWithNext().sumOf { (prev, next) -> (next - prev).coerceAtLeast(0.0) }

    return StravaSegmentEffortDetail(
        avgSpeedKmh = (velocitySmooth.average().takeIf { !it.isNaN() } ?: 0.0) * 3.6,
        maxSpeedKmh = (velocitySmooth.maxOrNull() ?: 0.0) * 3.6,
        elevationGainMeters = elevationGain,
        avgWatts = streamOrNull("watts")?.average(),
        avgHeartRateBpm = streamOrNull("heartrate")?.average(),
        avgCadenceRpm = streamOrNull("cadence")?.average(),
    )
}
