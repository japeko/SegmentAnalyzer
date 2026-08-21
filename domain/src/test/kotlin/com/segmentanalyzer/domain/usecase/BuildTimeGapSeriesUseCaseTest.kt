package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

private fun point(secondsFromStart: Long, distance: Double) = TrackPoint(
    latitude = 0.0,
    longitude = 0.0,
    elevationMeters = null,
    timestamp = Instant.EPOCH.plusSeconds(secondsFromStart),
    cumulativeDistanceMeters = distance,
)

class BuildTimeGapSeriesUseCaseTest {

    @Test
    fun `a uniformly slower attempt falls further behind with distance`() = runTest {
        val tracks = mapOf(
            1L to listOf(point(0, 0.0), point(10, 100.0)),
            2L to listOf(point(0, 0.0), point(20, 100.0)),
        )
        val useCase = BuildTimeGapSeriesUseCase(FakeTimeGapRepository(tracks))

        val series = useCase(referenceAttemptId = 1L, otherAttemptIds = listOf(2L), segmentDistanceMeters = 100.0, sampleCount = 5)

        val points = series.single { it.attemptId == 2L }.points
        assertEquals(listOf(0.0, 25.0, 50.0, 75.0, 100.0), points.map { it.distanceMeters })
        assertEquals(listOf(0.0, 2.5, 5.0, 7.5, 10.0), points.map { it.gapSeconds })
    }

    @Test
    fun `a uniformly faster attempt shows a negative gap`() = runTest {
        val tracks = mapOf(
            1L to listOf(point(0, 0.0), point(20, 100.0)),
            2L to listOf(point(0, 0.0), point(10, 100.0)),
        )
        val useCase = BuildTimeGapSeriesUseCase(FakeTimeGapRepository(tracks))

        val series = useCase(referenceAttemptId = 1L, otherAttemptIds = listOf(2L), segmentDistanceMeters = 100.0, sampleCount = 5)

        assertEquals(-10.0, series.single().points.last().gapSeconds, 0.001)
    }

    @Test
    fun `sampling beyond a shorter attempt's covered distance clamps to its last point`() = runTest {
        val tracks = mapOf(
            1L to listOf(point(0, 0.0), point(10, 100.0)),
            2L to listOf(point(0, 0.0), point(8, 80.0)),
        )
        val useCase = BuildTimeGapSeriesUseCase(FakeTimeGapRepository(tracks))

        val series = useCase(referenceAttemptId = 1L, otherAttemptIds = listOf(2L), segmentDistanceMeters = 100.0, sampleCount = 5)

        // At d=100, attempt 2 clamps to its last known elapsed time (8s) rather than extrapolating.
        assertEquals(8.0 - 10.0, series.single().points.last().gapSeconds, 0.001)
    }

    @Test
    fun `a non-monotonic GPS blip is dropped rather than breaking interpolation`() = runTest {
        val tracks = mapOf(
            1L to listOf(point(0, 0.0), point(10, 100.0)),
            // The point at distance 50 briefly dips back from 60 before continuing — should be skipped,
            // leaving a kept curve of (0,0) -> (60,10) -> (100,20).
            2L to listOf(point(0, 0.0), point(10, 60.0), point(11, 50.0), point(20, 100.0)),
        )
        val useCase = BuildTimeGapSeriesUseCase(FakeTimeGapRepository(tracks))

        val series = useCase(referenceAttemptId = 1L, otherAttemptIds = listOf(2L), segmentDistanceMeters = 100.0, sampleCount = 5)
        val points = series.single().points

        // d=25 interpolates within the first kept segment (0,0)->(60,10): 25/60*10, minus reference's 2.5s.
        assertEquals(25.0 / 60.0 * 10.0 - 2.5, points[1].gapSeconds, 0.001)
        // d=75 interpolates within the second kept segment (60,10)->(100,20), skipping the dropped dip.
        assertEquals(10.0 + 15.0 / 40.0 * 10.0 - 7.5, points[3].gapSeconds, 0.001)
    }
}

private class FakeTimeGapRepository(private val tracksByAttemptId: Map<Long, List<TrackPoint>>) : SegmentAttemptRepository {
    override fun observeAttemptsForSegment(segmentId: Long): Flow<List<SegmentAttempt>> = MutableStateFlow(emptyList())
    override fun observeMatchesForRide(rideId: Long) = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.RideSegmentMatch>())
    override suspend fun trackPointsForAttempt(attemptId: Long): List<TrackPoint> = tracksByAttemptId.getValue(attemptId)
    override suspend fun matchRideAgainstAllSegments(rideId: Long): Int = 0
    override suspend fun matchSegmentAgainstAllRides(segmentId: Long): Int = 0
    override suspend fun saveStravaEffortAttempt(
        segmentId: Long, rideId: Long, startTime: java.time.Instant, duration: java.time.Duration,
        avgSpeedKmh: Double, elevationGainMeters: Double, avgPowerWatts: Double?, effortExternalId: String,
    ) = Unit
}
