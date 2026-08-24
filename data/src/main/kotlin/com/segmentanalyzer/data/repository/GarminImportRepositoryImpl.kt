package com.segmentanalyzer.data.repository

import com.segmentanalyzer.common.DispatcherProvider
import com.segmentanalyzer.data.local.GarminSessionStore
import com.segmentanalyzer.data.remote.garmin.GarminActivityApi
import com.segmentanalyzer.data.remote.garmin.GarminActivityDetailsDto
import com.segmentanalyzer.data.remote.garmin.GarminActivityDto
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.GarminImportRepository
import com.segmentanalyzer.domain.repository.GarminSessionExpiredException
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

internal class GarminImportRepositoryImpl @Inject constructor(
    private val activityApi: GarminActivityApi,
    private val sessionStore: GarminSessionStore,
    private val dispatcherProvider: DispatcherProvider,
) : GarminImportRepository {

    override suspend fun fetchRecentRides(limit: Int, startDate: LocalDate?, endDate: LocalDate?): Result<List<Ride>> =
        withContext(dispatcherProvider.io) {
            runCatching {
                val session = sessionStore.session() ?: throw GarminSessionExpiredException()
                if (session.expiresAt.isBefore(Instant.now())) throw GarminSessionExpiredException()

                activityApi.fetchActivities(session.accessToken, limit, startDate, endDate).mapNotNull { it.toRideOrNull() }
            }
        }

    override suspend fun fetchTrack(externalId: String): List<TrackPoint> =
        withContext(dispatcherProvider.io) {
            val activityId = externalId.toLongOrNull() ?: return@withContext emptyList()
            runCatching {
                val session = sessionStore.session() ?: throw GarminSessionExpiredException()
                if (session.expiresAt.isBefore(Instant.now())) throw GarminSessionExpiredException()

                activityApi.fetchActivityDetails(session.accessToken, activityId).toTrackPoints()
            }.getOrElse { emptyList() }
        }
}

private val START_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

private fun GarminActivityDto.toRideOrNull(): Ride? {
    val activityType = activityType.typeKey.toActivityTypeOrNull() ?: return null
    return Ride(
        id = 0,
        name = activityName,
        activityType = activityType,
        source = ActivitySource.GARMIN,
        startTime = LocalDateTime.parse(startTimeLocal, START_TIME_FORMATTER)
            .atZone(ZoneId.systemDefault())
            .toInstant(),
        duration = Duration.ofSeconds(duration?.toLong() ?: 0L),
        distanceMeters = distance ?: 0.0,
        elevationGainMeters = elevationGain ?: 0.0,
        isPersonalBest = false,
        elevationProfile = emptyList(),
        sourceFilePath = null,
        externalId = activityId.toString(),
    )
}

/**
 * Turns Garmin's activity-details response into [TrackPoint]s, keyed dynamically by
 * [GarminActivityDetailsDto.metricDescriptors] rather than a fixed column order — confirmed live
 * that Garmin reorders `metricsIndex` between calls for the same activity. `directBikeCadence` and
 * `directPower` are unverified guesses at the field names (not present on the e-bike activity this
 * was tested against, which had no cadence/power sensor); a wrong guess just means those two
 * columns stay null, same as any other reverse-engineered field elsewhere in this file.
 */
private fun GarminActivityDetailsDto.toTrackPoints(): List<TrackPoint> {
    val indexOf = metricDescriptors.associate { it.key to it.metricsIndex }
    val latitudeIndex = indexOf["directLatitude"] ?: return emptyList()
    val longitudeIndex = indexOf["directLongitude"] ?: return emptyList()
    val timestampIndex = indexOf["directTimestamp"] ?: return emptyList()
    val elevationIndex = indexOf["directElevation"]
    val distanceIndex = indexOf["sumDistance"]
    val heartRateIndex = indexOf["directHeartRate"]
    val cadenceIndex = indexOf["directBikeCadence"]
    val powerIndex = indexOf["directPower"]

    return activityDetailMetrics.mapNotNull { row ->
        val metrics = row.metrics
        val latitude = metrics.getOrNull(latitudeIndex) ?: return@mapNotNull null
        val longitude = metrics.getOrNull(longitudeIndex) ?: return@mapNotNull null
        val timestampMillis = metrics.getOrNull(timestampIndex) ?: return@mapNotNull null
        TrackPoint(
            latitude = latitude,
            longitude = longitude,
            elevationMeters = elevationIndex?.let { metrics.getOrNull(it)?.toFloat() },
            timestamp = Instant.ofEpochMilli(timestampMillis.toLong()),
            cumulativeDistanceMeters = distanceIndex?.let { metrics.getOrNull(it) } ?: 0.0,
            heartRateBpm = heartRateIndex?.let { metrics.getOrNull(it)?.toInt() },
            cadenceRpm = cadenceIndex?.let { metrics.getOrNull(it)?.toInt() },
            powerWatts = powerIndex?.let { metrics.getOrNull(it)?.toInt() },
        )
    }
}

/**
 * Maps Garmin's activity type key to ours, or null if it isn't a bike ride at all.
 *
 * Deliberately broad so every bike variant Garmin has (indoor/virtual, e-bike, BMX, track,
 * recumbent, hand-cycling, ...) is imported as [ActivityType.OTHER] rather than silently
 * dropped just because it has no dedicated category here yet. "motorcycling" is excluded
 * explicitly since it would otherwise match on "cycling".
 */
internal fun String.toActivityTypeOrNull(): ActivityType? = when {
    contains("motorcyc") -> null
    contains("mountain") || contains("downhill") || contains("enduro") -> ActivityType.MTB
    contains("gravel") || contains("cyclocross") -> ActivityType.GRAVEL
    contains("road") || this == "cycling" -> ActivityType.ROAD
    contains("cycling") || contains("biking") || contains("bike") ||
        contains("virtual_ride") || contains("bmx") -> ActivityType.OTHER
    else -> null
}
