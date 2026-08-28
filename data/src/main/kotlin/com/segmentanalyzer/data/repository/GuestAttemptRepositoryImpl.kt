package com.segmentanalyzer.data.repository

import android.content.Context
import android.net.Uri
import com.segmentanalyzer.common.DispatcherProvider
import com.segmentanalyzer.data.local.dao.GuestAttemptDao
import com.segmentanalyzer.data.local.dao.SegmentDao
import com.segmentanalyzer.data.local.entity.GuestAttemptEntity
import com.segmentanalyzer.data.local.entity.GuestAttemptPointEntity
import com.segmentanalyzer.data.local.entity.SegmentEntity
import com.segmentanalyzer.data.local.fit.FitFileParser
import com.segmentanalyzer.data.local.fit.FitParseException
import com.segmentanalyzer.data.local.fit.FitTrackPoint
import com.segmentanalyzer.domain.model.GuestAttempt
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.GuestAttemptRepository
import com.segmentanalyzer.domain.util.SegmentMatchResult
import com.segmentanalyzer.domain.util.decodePolyline
import com.segmentanalyzer.domain.util.matchAllSegmentPasses
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

internal class GuestAttemptRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fitFileParser: FitFileParser,
    private val segmentDao: SegmentDao,
    private val guestAttemptDao: GuestAttemptDao,
    private val dispatcherProvider: DispatcherProvider,
) : GuestAttemptRepository {

    override suspend fun importFitFile(uri: String, riderName: String): Result<List<GuestAttempt>> =
        withContext(dispatcherProvider.io) {
            runCatching {
                val parsedUri = Uri.parse(uri)
                val summary = context.contentResolver.openInputStream(parsedUri)?.use { fitFileParser.parse(it) }
                    ?: throw FitParseException("couldn't open the selected file")
                val track = summary.trackPoints.map { it.toDomain() }
                if (track.isEmpty()) throw FitParseException("this FIT file has no GPS track to match against your segments")

                val importedAt = Instant.now()
                val saved = segmentDao.getAll().flatMap { segment ->
                    segment.matches(track).map { match -> saveMatch(segment, match, track, riderName, importedAt) }
                }
                if (saved.isEmpty()) throw FitParseException("this ride doesn't cross any of your segments")
                saved
            }
        }

    override fun observeForSegment(segmentId: Long): Flow<List<GuestAttempt>> =
        guestAttemptDao.observeForSegment(segmentId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun trackPointsForGuestAttempt(guestAttemptId: Long): List<TrackPoint> =
        withContext(dispatcherProvider.io) {
            guestAttemptDao.pointsForAttempt(guestAttemptId).map { it.toDomain() }
        }

    override suspend fun deleteGuestAttempt(id: Long) = withContext(dispatcherProvider.io) {
        guestAttemptDao.delete(id)
    }

    private suspend fun saveMatch(
        segment: SegmentEntity,
        match: SegmentMatchResult,
        track: List<TrackPoint>,
        riderName: String,
        importedAt: Instant,
    ): GuestAttempt {
        val attemptId = guestAttemptDao.insert(
            GuestAttemptEntity(
                segmentId = segment.id,
                riderName = riderName,
                importedAtEpochMillis = importedAt.toEpochMilli(),
                startTimeEpochMillis = match.startTime.toEpochMilli(),
                durationMillis = match.duration.toMillis(),
                avgSpeedKmh = match.avgSpeedKmh,
                elevationGainMeters = match.elevationGainMeters,
            ),
        )
        val baseDistance = track[match.entryIndex].cumulativeDistanceMeters
        val points = (match.entryIndex..match.exitIndex).mapIndexed { sequence, trackIndex ->
            track[trackIndex].toEntity(attemptId, sequence, baseDistance)
        }
        guestAttemptDao.insertPoints(points)
        return GuestAttempt(
            id = attemptId,
            segmentId = segment.id,
            riderName = riderName,
            importedAt = importedAt,
            startTime = match.startTime,
            duration = match.duration,
            avgSpeedKmh = match.avgSpeedKmh,
            elevationGainMeters = match.elevationGainMeters,
        )
    }
}

private fun SegmentEntity.matches(track: List<TrackPoint>): List<SegmentMatchResult> {
    val startLat = startLatitude
    val startLon = startLongitude
    val endLat = endLatitude
    val endLon = endLongitude
    if (startLat == null || startLon == null || endLat == null || endLon == null) return emptyList()

    val decodedPolyline = polyline?.let { decodePolyline(it) }.orEmpty()
    return matchAllSegmentPasses(
        track, startLat, startLon, endLat, endLon,
        segmentDistanceMeters = distanceMeters,
        polyline = decodedPolyline,
    )
}

private fun GuestAttemptEntity.toDomain(): GuestAttempt = GuestAttempt(
    id = id,
    segmentId = segmentId,
    riderName = riderName,
    importedAt = Instant.ofEpochMilli(importedAtEpochMillis),
    startTime = Instant.ofEpochMilli(startTimeEpochMillis),
    duration = Duration.ofMillis(durationMillis),
    avgSpeedKmh = avgSpeedKmh,
    elevationGainMeters = elevationGainMeters,
)

private fun GuestAttemptPointEntity.toDomain(): TrackPoint = TrackPoint(
    latitude = latitude,
    longitude = longitude,
    elevationMeters = elevationMeters,
    timestamp = Instant.ofEpochMilli(timestampEpochMillis),
    cumulativeDistanceMeters = cumulativeDistanceMeters,
    heartRateBpm = heartRateBpm,
    cadenceRpm = cadenceRpm,
    powerWatts = powerWatts,
)

private fun TrackPoint.toEntity(guestAttemptId: Long, sequence: Int, baseDistanceMeters: Double) = GuestAttemptPointEntity(
    guestAttemptId = guestAttemptId,
    sequence = sequence,
    latitude = latitude,
    longitude = longitude,
    elevationMeters = elevationMeters,
    timestampEpochMillis = timestamp.toEpochMilli(),
    cumulativeDistanceMeters = cumulativeDistanceMeters - baseDistanceMeters,
    heartRateBpm = heartRateBpm,
    cadenceRpm = cadenceRpm,
    powerWatts = powerWatts,
)

private fun FitTrackPoint.toDomain(): TrackPoint = TrackPoint(
    latitude = latitude,
    longitude = longitude,
    elevationMeters = elevationMeters,
    timestamp = timestamp,
    cumulativeDistanceMeters = cumulativeDistanceMeters,
    heartRateBpm = heartRateBpm,
    cadenceRpm = cadenceRpm,
    powerWatts = powerWatts,
)
