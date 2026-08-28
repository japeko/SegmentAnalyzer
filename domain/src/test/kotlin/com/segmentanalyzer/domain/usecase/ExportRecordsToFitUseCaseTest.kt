package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.RideSegmentMatch
import com.segmentanalyzer.domain.model.SegmentRecord
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.FitExportRepository
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.time.Duration
import java.time.Instant

private fun record(attemptId: Long, segmentName: String) = SegmentRecord(
    attemptId = attemptId,
    segmentId = attemptId,
    segmentName = segmentName,
    segmentDistanceMeters = 1_000.0,
    rideId = attemptId,
    rideName = "Ride $attemptId",
    rideSource = ActivitySource.FIT_FILE,
    startTime = Instant.parse("2026-08-16T06:00:00Z"),
    duration = Duration.ofMinutes(5),
    avgSpeedKmh = 20.0,
)

private fun point() = TrackPoint(
    latitude = 61.0,
    longitude = 24.0,
    elevationMeters = 100f,
    timestamp = Instant.EPOCH,
    cumulativeDistanceMeters = 0.0,
)

class ExportRecordsToFitUseCaseTest {

    @Test
    fun `exports every record that has a recorded track`() = runTest {
        val tracks = mapOf(1L to listOf(point()), 2L to listOf(point(), point()))
        val attemptRepository = FakeExportSegmentAttemptRepository(tracks)
        val exportRepository = FakeFitExportRepository()
        val useCase = ExportRecordsToFitUseCase(attemptRepository, exportRepository)

        val result = useCase(listOf(record(1, "Skyline Climb"), record(2, "Widow Creek")))

        assertEquals(2, result.exportedFiles.size)
        assertEquals(0, result.skippedCount)
        assertEquals(listOf("Skyline Climb", "Widow Creek"), exportRepository.exportedSegmentNames)
    }

    @Test
    fun `skips a record with no recorded track instead of failing the whole export`() = runTest {
        val tracks = mapOf(1L to listOf(point()), 2L to emptyList())
        val attemptRepository = FakeExportSegmentAttemptRepository(tracks)
        val exportRepository = FakeFitExportRepository()
        val useCase = ExportRecordsToFitUseCase(attemptRepository, exportRepository)

        val result = useCase(listOf(record(1, "Skyline Climb"), record(2, "No Track Segment")))

        assertEquals(1, result.exportedFiles.size)
        assertEquals(1, result.skippedCount)
        assertEquals(listOf("Skyline Climb"), exportRepository.exportedSegmentNames)
    }
}

private class FakeExportSegmentAttemptRepository(
    private val tracksByAttemptId: Map<Long, List<TrackPoint>>,
) : SegmentAttemptRepository {
    override fun observeAttemptsForSegment(segmentId: Long) = throw UnsupportedOperationException("not used in this test")
    override fun observeMatchesForRide(rideId: Long): Flow<List<RideSegmentMatch>> = MutableStateFlow(emptyList())
    override fun observeImportedStravaEffortIds(rideId: Long) = MutableStateFlow(emptySet<String>())
    override fun observeRecords() = MutableStateFlow(emptyList<SegmentRecord>())
    override suspend fun trackPointsForAttempt(attemptId: Long): List<TrackPoint> = tracksByAttemptId[attemptId].orEmpty()
    override suspend fun matchRideAgainstAllSegments(rideId: Long): Int = 0
    override suspend fun matchSegmentAgainstAllRides(segmentId: Long): Int = 0
    override suspend fun saveStravaEffortAttempt(
        segmentId: Long, rideId: Long, startTime: Instant, duration: Duration,
        avgSpeedKmh: Double, elevationGainMeters: Double, avgPowerWatts: Double?, effortExternalId: String,
    ) = throw UnsupportedOperationException("not used in this test")
}

private class FakeFitExportRepository : FitExportRepository {
    val exportedSegmentNames = mutableListOf<String>()

    override suspend fun exportRecord(record: SegmentRecord, points: List<TrackPoint>): File {
        exportedSegmentNames += record.segmentName
        return File("${record.segmentName}.fit")
    }
}
