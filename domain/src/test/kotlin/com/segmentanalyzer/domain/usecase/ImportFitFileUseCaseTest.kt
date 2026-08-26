package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.FitFileRepository
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
    name = "Morning Ride",
    activityType = ActivityType.ROAD,
    source = ActivitySource.FIT_FILE,
    startTime = Instant.now(),
    duration = Duration.ofMinutes(45),
    distanceMeters = 15_000.0,
    elevationGainMeters = 200.0,
    isPersonalBest = false,
    elevationProfile = emptyList(),
    sourceFilePath = "content://fake/morning-ride.fit",
)

class ImportFitFileUseCaseTest {

    @Test
    fun `saves the parsed ride and returns it`() = runTest {
        val rideRepository = FakeFitRideRepository()
        val useCase = ImportFitFileUseCase(
            FakeFitFileRepository(Result.success(parsedRide)),
            rideRepository,
            MatchNewRideToSegmentsUseCase(FakeFitSegmentAttemptRepository()),
        )

        val result = useCase("content://fake/morning-ride.fit")

        assertTrue(result.isSuccess)
        assertEquals(parsedRide, result.getOrNull())
        assertEquals(listOf(parsedRide), rideRepository.savedRides)
    }

    @Test
    fun `surfaces a parse failure without saving anything`() = runTest {
        val rideRepository = FakeFitRideRepository()
        val useCase = ImportFitFileUseCase(
            FakeFitFileRepository(Result.failure(IllegalStateException("not a cycling activity"))),
            rideRepository,
            MatchNewRideToSegmentsUseCase(FakeFitSegmentAttemptRepository()),
        )

        val result = useCase("content://fake/run.fit")

        assertTrue(result.isFailure)
        assertEquals("not a cycling activity", result.exceptionOrNull()?.message)
        assertTrue(rideRepository.savedRides.isEmpty())
    }
}

private class FakeFitFileRepository(private val result: Result<Ride>) : FitFileRepository {
    override suspend fun parseFitFile(uri: String): Result<Ride> = result
}

private class FakeFitRideRepository : RideRepository {
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

    override suspend fun updateRide(rideId: Long, name: String, tag: String?, activityType: ActivityType) = Unit
    override suspend fun setTagForRides(rideIds: List<Long>, tag: String?) = Unit
    override suspend fun setActivityTypeForRides(rideIds: List<Long>, activityType: ActivityType) = Unit
    override suspend fun deleteRide(rideId: Long) = Unit
    override fun observeAllTags(): Flow<List<String>> = MutableStateFlow(emptyList())
}

private class FakeFitSegmentAttemptRepository : SegmentAttemptRepository {
    override fun observeAttemptsForSegment(segmentId: Long) = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.SegmentAttempt>())
    override fun observeMatchesForRide(rideId: Long) = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.RideSegmentMatch>())
    override fun observeRecords() = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.SegmentRecord>())
    override suspend fun trackPointsForAttempt(attemptId: Long) = emptyList<com.segmentanalyzer.domain.model.TrackPoint>()
    override suspend fun matchRideAgainstAllSegments(rideId: Long) = 0
    override suspend fun matchSegmentAgainstAllRides(segmentId: Long) = 0
    override suspend fun saveStravaEffortAttempt(
        segmentId: Long, rideId: Long, startTime: java.time.Instant, duration: java.time.Duration,
        avgSpeedKmh: Double, elevationGainMeters: Double, avgPowerWatts: Double?, effortExternalId: String,
    ) = Unit
    override suspend fun hasLocalAttempt(segmentId: Long, rideId: Long) = false
}
