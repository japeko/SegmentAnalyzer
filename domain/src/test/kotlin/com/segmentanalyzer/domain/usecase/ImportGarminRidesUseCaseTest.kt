package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.RideSegmentMatch
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.GarminImportRepository
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

private fun ride(externalId: String) = Ride(
    id = 0,
    name = "Imported Ride $externalId",
    activityType = ActivityType.MTB,
    source = ActivitySource.GARMIN,
    startTime = Instant.now(),
    duration = Duration.ofMinutes(30),
    distanceMeters = 10_000.0,
    elevationGainMeters = 100.0,
    isPersonalBest = false,
    elevationProfile = emptyList(),
    sourceFilePath = null,
    externalId = externalId,
)

private fun trackPoint() = TrackPoint(
    latitude = 61.5,
    longitude = 23.8,
    elevationMeters = 100f,
    timestamp = Instant.now(),
    cumulativeDistanceMeters = 0.0,
)

class ImportGarminRidesUseCaseTest {

    @Test
    fun `saves each ride with its fetched track and reports how many were new`() = runTest {
        val selected = listOf(ride("1"), ride("2"), ride("3"))
        val rideRepository = FakeImportRideRepository(newRideIds = listOf(101L, null, 103L))
        val garminImportRepository = FakeGarminTrackRepository(tracksByExternalId = mapOf("1" to listOf(trackPoint())))
        val segmentAttemptRepository = FakeSegmentAttemptRepository()
        val useCase = ImportGarminRidesUseCase(
            garminImportRepository,
            rideRepository,
            MatchNewRideToSegmentsUseCase(segmentAttemptRepository),
        )

        val result = useCase(selected)

        assertTrue(result.isSuccess)
        assertEquals(ImportSummary(selectedCount = 3, importedCount = 2), result.getOrNull())
        // Ride "1" had a fetched track; rides "2"/"3" didn't, so they saved with an empty track.
        assertEquals(1, rideRepository.savedTracks["1"]?.size)
        assertEquals(0, rideRepository.savedTracks["2"]?.size)
        // Matching only runs for rides that actually saved (ids 101 and 103, not the skipped duplicate).
        assertEquals(listOf(101L, 103L), segmentAttemptRepository.matchedRideIds)
    }

    @Test
    fun `an empty selection saves nothing and reports zero counts`() = runTest {
        val rideRepository = FakeImportRideRepository(newRideIds = emptyList())
        val garminImportRepository = FakeGarminTrackRepository(tracksByExternalId = emptyMap())
        val segmentAttemptRepository = FakeSegmentAttemptRepository()
        val useCase = ImportGarminRidesUseCase(
            garminImportRepository,
            rideRepository,
            MatchNewRideToSegmentsUseCase(segmentAttemptRepository),
        )

        val result = useCase(emptyList())

        assertTrue(result.isSuccess)
        assertEquals(ImportSummary(selectedCount = 0, importedCount = 0), result.getOrNull())
        assertTrue(segmentAttemptRepository.matchedRideIds.isEmpty())
    }
}

private class FakeGarminTrackRepository(private val tracksByExternalId: Map<String, List<TrackPoint>>) : GarminImportRepository {
    override suspend fun fetchRecentRides(limit: Int, startDate: java.time.LocalDate?, endDate: java.time.LocalDate?): Result<List<Ride>> =
        Result.success(emptyList())

    override suspend fun fetchTrack(externalId: String): List<TrackPoint> = tracksByExternalId[externalId].orEmpty()
}

/** Returns each queued id from [newRideIds] in order (null = a duplicate that didn't save), keyed by the saved ride's externalId. */
private class FakeImportRideRepository(newRideIds: List<Long?>) : RideRepository {
    private val queue = ArrayDeque(newRideIds)
    val savedTracks = mutableMapOf<String, List<TrackPoint>>()

    override fun observeRides(): Flow<List<Ride>> = MutableStateFlow(emptyList())
    override fun observeRide(rideId: Long): Flow<Ride?> = MutableStateFlow(null)
    override fun observeHasTrack(rideId: Long): Flow<Boolean> = MutableStateFlow(false)
    override suspend fun saveRides(rides: List<Ride>): Int = 0

    override suspend fun saveRide(ride: Ride): Long? {
        ride.externalId?.let { savedTracks[it] = ride.track }
        return queue.removeFirstOrNull()
    }

    override suspend fun updateRide(rideId: Long, name: String, tag: String?, activityType: ActivityType) = Unit
    override suspend fun setTagForRides(rideIds: List<Long>, tag: String?) = Unit
    override suspend fun setActivityTypeForRides(rideIds: List<Long>, activityType: ActivityType) = Unit
    override fun observeAllTags(): Flow<List<String>> = MutableStateFlow(emptyList())
}

private class FakeSegmentAttemptRepository : SegmentAttemptRepository {
    val matchedRideIds = mutableListOf<Long>()

    override fun observeAttemptsForSegment(segmentId: Long): Flow<List<SegmentAttempt>> = MutableStateFlow(emptyList())
    override fun observeMatchesForRide(rideId: Long): Flow<List<RideSegmentMatch>> = MutableStateFlow(emptyList())
    override suspend fun trackPointsForAttempt(attemptId: Long): List<TrackPoint> = emptyList()

    override suspend fun matchRideAgainstAllSegments(rideId: Long): Int {
        matchedRideIds += rideId
        return 0
    }

    override suspend fun matchSegmentAgainstAllRides(segmentId: Long): Int = 0

    override suspend fun saveStravaEffortAttempt(
        segmentId: Long,
        rideId: Long,
        startTime: Instant,
        duration: Duration,
        avgSpeedKmh: Double,
        elevationGainMeters: Double,
        avgPowerWatts: Double?,
        effortExternalId: String,
    ) = Unit

    override suspend fun hasLocalAttempt(segmentId: Long, rideId: Long): Boolean = false
}
