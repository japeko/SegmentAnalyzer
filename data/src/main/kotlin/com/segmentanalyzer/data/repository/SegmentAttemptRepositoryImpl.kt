package com.segmentanalyzer.data.repository

import com.segmentanalyzer.common.DispatcherProvider
import com.segmentanalyzer.data.local.dao.RidePointDao
import com.segmentanalyzer.data.local.dao.SegmentAttemptDao
import com.segmentanalyzer.data.local.dao.SegmentAttemptWithRide
import com.segmentanalyzer.data.local.dao.SegmentDao
import com.segmentanalyzer.data.local.entity.RidePointEntity
import com.segmentanalyzer.data.local.entity.SegmentAttemptEntity
import com.segmentanalyzer.data.local.entity.SegmentEntity
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.util.decodePolyline
import com.segmentanalyzer.domain.util.matchSegment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

internal class SegmentAttemptRepositoryImpl @Inject constructor(
    private val ridePointDao: RidePointDao,
    private val segmentAttemptDao: SegmentAttemptDao,
    private val segmentDao: SegmentDao,
    private val dispatcherProvider: DispatcherProvider,
) : SegmentAttemptRepository {

    override fun observeAttemptsForSegment(segmentId: Long): Flow<List<SegmentAttempt>> =
        segmentAttemptDao.observeForSegment(segmentId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun trackPointsForAttempt(attemptId: Long): List<TrackPoint> =
        withContext(dispatcherProvider.io) {
            val attempt = segmentAttemptDao.attemptById(attemptId) ?: return@withContext emptyList()
            val points = ridePointDao.pointsForRide(attempt.rideId)
            val subTrack = points.subList(
                attempt.entryPointSequence.coerceIn(0, points.size),
                (attempt.exitPointSequence + 1).coerceIn(0, points.size),
            )
            val baseDistance = subTrack.firstOrNull()?.cumulativeDistanceMeters ?: 0.0
            subTrack.map { it.toDomain(baseDistance) }
        }

    override suspend fun matchRideAgainstAllSegments(rideId: Long): Int =
        withContext(dispatcherProvider.io) {
            val points = ridePointDao.pointsForRide(rideId)
            if (points.isEmpty()) return@withContext 0
            val track = points.map { it.toDomain(0.0) }

            val matches = segmentDao.getAll().mapNotNull { segment -> segment.toAttemptOrNull(rideId, track) }
            segmentAttemptDao.insertIfNew(matches).count { it != -1L }
        }

    override suspend fun matchSegmentAgainstAllRides(segmentId: Long): Int =
        withContext(dispatcherProvider.io) {
            val segment = segmentDao.getById(segmentId) ?: return@withContext 0
            val rideIds = ridePointDao.rideIdsWithTracks()

            val matches = rideIds.mapNotNull { rideId ->
                val track = ridePointDao.pointsForRide(rideId).map { it.toDomain(0.0) }
                segment.toAttemptOrNull(rideId, track)
            }
            segmentAttemptDao.insertIfNew(matches).count { it != -1L }
        }
}

private fun SegmentEntity.toAttemptOrNull(rideId: Long, track: List<TrackPoint>): SegmentAttemptEntity? {
    val startLat = startLatitude
    val startLon = startLongitude
    val endLat = endLatitude
    val endLon = endLongitude
    if (startLat == null || startLon == null || endLat == null || endLon == null) return null

    val decodedPolyline = polyline?.let { decodePolyline(it) }.orEmpty()
    val match = matchSegment(track, startLat, startLon, endLat, endLon, polyline = decodedPolyline) ?: return null
    return SegmentAttemptEntity(
        segmentId = id,
        rideId = rideId,
        startTimeEpochMillis = match.startTime.toEpochMilli(),
        durationMillis = match.duration.toMillis(),
        avgSpeedKmh = match.avgSpeedKmh,
        elevationGainMeters = match.elevationGainMeters,
        avgPowerWatts = match.avgPowerWatts,
        entryPointSequence = match.entryIndex,
        exitPointSequence = match.exitIndex,
        createdAtEpochMillis = Instant.now().toEpochMilli(),
    )
}

private fun RidePointEntity.toDomain(baseDistanceMeters: Double): TrackPoint = TrackPoint(
    latitude = latitude,
    longitude = longitude,
    elevationMeters = elevationMeters,
    timestamp = Instant.ofEpochMilli(timestampEpochMillis),
    cumulativeDistanceMeters = cumulativeDistanceMeters - baseDistanceMeters,
    heartRateBpm = heartRateBpm,
    cadenceRpm = cadenceRpm,
    powerWatts = powerWatts,
)

private fun SegmentAttemptWithRide.toDomain(): SegmentAttempt = SegmentAttempt(
    id = attempt.id,
    segmentId = attempt.segmentId,
    rideId = attempt.rideId,
    rideName = rideName,
    rideSource = rideSource,
    startTime = Instant.ofEpochMilli(attempt.startTimeEpochMillis),
    duration = Duration.ofMillis(attempt.durationMillis),
    avgSpeedKmh = attempt.avgSpeedKmh,
    elevationGainMeters = attempt.elevationGainMeters,
    avgPowerWatts = attempt.avgPowerWatts,
)
