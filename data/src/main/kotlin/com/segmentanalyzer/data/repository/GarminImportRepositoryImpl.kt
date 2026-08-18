package com.segmentanalyzer.data.repository

import com.segmentanalyzer.common.DispatcherProvider
import com.segmentanalyzer.data.local.GarminSessionStore
import com.segmentanalyzer.data.remote.garmin.GarminActivityApi
import com.segmentanalyzer.data.remote.garmin.GarminActivityDto
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.GarminImportRepository
import com.segmentanalyzer.domain.repository.GarminSessionExpiredException
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

internal class GarminImportRepositoryImpl @Inject constructor(
    private val activityApi: GarminActivityApi,
    private val sessionStore: GarminSessionStore,
    private val dispatcherProvider: DispatcherProvider,
) : GarminImportRepository {

    override suspend fun fetchRecentRides(limit: Int): Result<List<Ride>> =
        withContext(dispatcherProvider.io) {
            runCatching {
                val session = sessionStore.session() ?: throw GarminSessionExpiredException()
                if (session.expiresAt.isBefore(Instant.now())) throw GarminSessionExpiredException()

                activityApi.fetchActivities(session.accessToken, limit).mapNotNull { it.toRideOrNull() }
            }
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

/** Maps Garmin's activity type key to ours, or null if it isn't a cycling activity at all. */
private fun String.toActivityTypeOrNull(): ActivityType? = when {
    contains("mountain") || contains("downhill") || contains("enduro") -> ActivityType.MTB
    contains("gravel") || contains("cyclocross") -> ActivityType.GRAVEL
    contains("road") || this == "cycling" -> ActivityType.ROAD
    contains("cycling") || contains("biking") || contains("bike") -> ActivityType.OTHER
    else -> null
}
