package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Segment
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.repository.SegmentRepository
import com.segmentanalyzer.domain.repository.StravaSegmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun segment(externalId: String) = Segment(
    id = 0,
    externalId = externalId,
    name = "Segment $externalId",
    distanceMeters = 2_500.0,
    averageGradePercent = 4.5,
    maximumGradePercent = 12.0,
    elevationGainMeters = 110.0,
    climbCategory = 2,
    city = "Tampere",
    state = "Pirkanmaa",
)

class SyncStravaSegmentsUseCaseTest {

    @Test
    fun `syncs fetched segments and reports how many were new`() = runTest {
        val fetched = listOf(segment("1"), segment("2"), segment("3"))
        val useCase = SyncStravaSegmentsUseCase(
            FakeStravaSegmentRepository(Result.success(fetched)),
            FakeSegmentRepository(newCount = 2),
            MatchNewSegmentsToRidesUseCase(FakeSyncSegmentAttemptRepository()),
        )

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(SegmentSyncSummary(fetchedCount = 3, syncedCount = 2), result.getOrNull())
    }

    @Test
    fun `surfaces a fetch failure without touching the segment repository`() = runTest {
        val segmentRepository = FakeSegmentRepository(newCount = 0)
        val useCase = SyncStravaSegmentsUseCase(
            FakeStravaSegmentRepository(Result.failure(IllegalStateException("session expired"))),
            segmentRepository,
            MatchNewSegmentsToRidesUseCase(FakeSyncSegmentAttemptRepository()),
        )

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("session expired", result.exceptionOrNull()?.message)
        assertEquals(0, segmentRepository.saveCallCount)
    }
}

private class FakeStravaSegmentRepository(private val result: Result<List<Segment>>) : StravaSegmentRepository {
    override suspend fun fetchStarredSegments(): Result<List<Segment>> = result
}

private class FakeSegmentRepository(private val newCount: Int) : SegmentRepository {
    var saveCallCount = 0
        private set

    override fun observeSegments(): Flow<List<Segment>> = MutableStateFlow(emptyList())

    override suspend fun saveSegments(segments: List<Segment>): List<Long> {
        saveCallCount++
        return (1..newCount).map { it.toLong() }
    }
}

private class FakeSyncSegmentAttemptRepository : SegmentAttemptRepository {
    override fun observeAttemptsForSegment(segmentId: Long): Flow<List<SegmentAttempt>> = MutableStateFlow(emptyList())
    override fun observeMatchesForRide(rideId: Long) = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.RideSegmentMatch>())
    override suspend fun trackPointsForAttempt(attemptId: Long): List<TrackPoint> = emptyList()
    override suspend fun matchRideAgainstAllSegments(rideId: Long): Int = 0
    override suspend fun matchSegmentAgainstAllRides(segmentId: Long): Int = 0
    override suspend fun saveStravaEffortAttempt(
        segmentId: Long, rideId: Long, startTime: java.time.Instant, duration: java.time.Duration,
        avgSpeedKmh: Double, elevationGainMeters: Double, avgPowerWatts: Double?, effortExternalId: String,
    ) = Unit
}
