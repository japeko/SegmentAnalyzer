package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.GpxFileRepository
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

private val parsedRide = Ride(
    id = 0,
    name = "Evening Loop",
    activityType = ActivityType.GRAVEL,
    source = ActivitySource.GPX_FILE,
    startTime = Instant.now(),
    duration = Duration.ofMinutes(60),
    distanceMeters = 20_000.0,
    elevationGainMeters = 300.0,
    isPersonalBest = false,
    elevationProfile = emptyList(),
    sourceFilePath = "content://fake/evening-loop.gpx",
)

class ImportGpxFileUseCaseTest {

    @Test
    fun `saves the parsed ride and returns it`() = runTest {
        val rideRepository = FakeGpxRideRepository()
        val useCase = ImportGpxFileUseCase(
            FakeGpxFileRepository(Result.success(parsedRide)),
            rideRepository,
            MatchNewRideToSegmentsUseCase(FakeGpxSegmentAttemptRepository()),
        )

        val result = useCase("content://fake/evening-loop.gpx")

        assertTrue(result.isSuccess)
        assertEquals(parsedRide, result.getOrNull())
        assertEquals(listOf(parsedRide), rideRepository.savedRides)
    }

    @Test
    fun `surfaces a parse failure without saving anything`() = runTest {
        val rideRepository = FakeGpxRideRepository()
        val useCase = ImportGpxFileUseCase(
            FakeGpxFileRepository(Result.failure(IllegalStateException("this GPX file isn't a cycling activity"))),
            rideRepository,
            MatchNewRideToSegmentsUseCase(FakeGpxSegmentAttemptRepository()),
        )

        val result = useCase("content://fake/hike.gpx")

        assertTrue(result.isFailure)
        assertEquals("this GPX file isn't a cycling activity", result.exceptionOrNull()?.message)
        assertTrue(rideRepository.savedRides.isEmpty())
    }
}

private class FakeGpxFileRepository(private val result: Result<Ride>) : GpxFileRepository {
    override suspend fun parseGpxFile(uri: String): Result<Ride> = result
}

private class FakeGpxRideRepository : RideRepository {
    val savedRides = mutableListOf<Ride>()
    private var nextId = 1L

    override fun observeRides(): Flow<List<Ride>> = MutableStateFlow(emptyList())
    override fun observeRide(rideId: Long): Flow<Ride?> = MutableStateFlow(null)
    override fun observeHasTrack(rideId: Long): Flow<Boolean> = MutableStateFlow(false)

    override suspend fun saveRides(rides: List<Ride>): Int {
        savedRides += rides
        return rides.size
    }

    override suspend fun saveRide(ride: Ride): Long {
        savedRides += ride
        return nextId++
    }
}

private class FakeGpxSegmentAttemptRepository : SegmentAttemptRepository {
    override fun observeAttemptsForSegment(segmentId: Long) = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.SegmentAttempt>())
    override fun observeMatchesForRide(rideId: Long) = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.RideSegmentMatch>())
    override suspend fun trackPointsForAttempt(attemptId: Long) = emptyList<com.segmentanalyzer.domain.model.TrackPoint>()
    override suspend fun matchRideAgainstAllSegments(rideId: Long) = 0
    override suspend fun matchSegmentAgainstAllRides(segmentId: Long) = 0
    override suspend fun saveStravaEffortAttempt(
        segmentId: Long, rideId: Long, startTime: java.time.Instant, duration: java.time.Duration,
        avgSpeedKmh: Double, elevationGainMeters: Double, avgPowerWatts: Double?, effortExternalId: String,
    ) = Unit
    override suspend fun hasLocalAttempt(segmentId: Long, rideId: Long) = false
}
