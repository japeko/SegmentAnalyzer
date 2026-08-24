package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.RideSegmentMatch
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.Segment
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.model.StravaSegmentEffortDetail
import com.segmentanalyzer.domain.model.StravaSegmentEffortPoint
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.repository.SegmentRepository
import com.segmentanalyzer.domain.repository.StravaSegmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

private val segment = Segment(
    id = 7,
    externalId = "strava-seg-1",
    name = "HaiKala",
    distanceMeters = 1_500.0,
    averageGradePercent = 4.0,
    maximumGradePercent = 12.0,
    elevationGainMeters = 60.0,
    climbCategory = 0,
    city = "Tampere",
    state = null,
)

private val ride = Ride(
    id = 42,
    name = "Evening Loop",
    activityType = ActivityType.MTB,
    source = ActivitySource.GARMIN,
    startTime = Instant.parse("2026-08-18T06:15:00Z"),
    duration = Duration.ofMinutes(90),
    distanceMeters = 25_000.0,
    elevationGainMeters = 500.0,
    isPersonalBest = false,
    elevationProfile = emptyList(),
    sourceFilePath = null,
)

private val effort = StravaSegmentEffort(
    effortExternalId = "effort-1",
    segmentExternalId = segment.externalId,
    segmentName = segment.name,
    elapsedTime = Duration.ofMinutes(5),
    distanceMeters = segment.distanceMeters,
    komRank = null,
    prRank = 2,
)

private val detail = StravaSegmentEffortDetail(
    avgSpeedKmh = 24.5,
    maxSpeedKmh = 38.0,
    elevationGainMeters = 12.0,
    avgWatts = 210.0,
    avgHeartRateBpm = 152.0,
    avgCadenceRpm = 78.0,
    track = listOf(StravaSegmentEffortPoint(timeSeconds = 0, distanceMeters = 0.0, latitude = 60.1, longitude = 24.9)),
)

class SaveStravaSegmentEffortAttemptUseCaseTest {

    @Test
    fun `saves a pseudo-attempt when the segment and ride are both known locally`() = runTest {
        val attemptRepository = FakeSaveAttemptSegmentAttemptRepository()
        val useCase = SaveStravaSegmentEffortAttemptUseCase(
            FakeSaveAttemptSegmentRepository(listOf(segment)),
            FakeSaveAttemptStravaSegmentRepository(),
            FakeSaveAttemptRideRepository(ride),
            attemptRepository,
            MatchNewSegmentsToRidesUseCase(attemptRepository),
        )

        useCase(ride.id, effort, detail)

        val saved = attemptRepository.saved.single()
        assertEquals(segment.id, saved.segmentId)
        assertEquals(ride.id, saved.rideId)
        assertEquals(ride.startTime, saved.startTime)
        assertEquals(effort.elapsedTime, saved.duration)
        assertEquals(detail.avgSpeedKmh, saved.avgSpeedKmh, 0.0)
        assertEquals(detail.elevationGainMeters, saved.elevationGainMeters, 0.0)
        assertEquals(detail.avgWatts, saved.avgPowerWatts)
        assertEquals(effort.effortExternalId, saved.effortExternalId)
    }

    @Test
    fun `fetches and saves the segment from Strava first when it isn't synced locally yet`() = runTest {
        val attemptRepository = FakeSaveAttemptSegmentAttemptRepository()
        val segmentRepository = FakeSaveAttemptSegmentRepository(emptyList())
        val useCase = SaveStravaSegmentEffortAttemptUseCase(
            segmentRepository,
            FakeSaveAttemptStravaSegmentRepository(Result.success(segment)),
            FakeSaveAttemptRideRepository(ride),
            attemptRepository,
            MatchNewSegmentsToRidesUseCase(attemptRepository),
        )

        useCase(ride.id, effort, detail)

        assertEquals(listOf(segment.externalId), segmentRepository.savedExternalIds)
        val saved = attemptRepository.saved.single()
        assertEquals(segmentRepository.assignedId, saved.segmentId)
        assertTrue("expected the newly-fetched segment to be matched against local rides", attemptRepository.matchedSegmentIds.contains(segmentRepository.assignedId))
    }

    @Test
    fun `is a no-op when the segment isn't known locally and the Strava fetch fails`() = runTest {
        val attemptRepository = FakeSaveAttemptSegmentAttemptRepository()
        val useCase = SaveStravaSegmentEffortAttemptUseCase(
            FakeSaveAttemptSegmentRepository(emptyList()),
            FakeSaveAttemptStravaSegmentRepository(Result.failure(IllegalStateException("not found"))),
            FakeSaveAttemptRideRepository(ride),
            attemptRepository,
            MatchNewSegmentsToRidesUseCase(attemptRepository),
        )

        useCase(ride.id, effort, detail)

        assertTrue(attemptRepository.saved.isEmpty())
    }

    @Test
    fun `is a no-op when the ride isn't found`() = runTest {
        val attemptRepository = FakeSaveAttemptSegmentAttemptRepository()
        val useCase = SaveStravaSegmentEffortAttemptUseCase(
            FakeSaveAttemptSegmentRepository(listOf(segment)),
            FakeSaveAttemptStravaSegmentRepository(),
            FakeSaveAttemptRideRepository(ride = null),
            attemptRepository,
            MatchNewSegmentsToRidesUseCase(attemptRepository),
        )

        useCase(ride.id, effort, detail)

        assertTrue(attemptRepository.saved.isEmpty())
    }

    @Test
    fun `is a no-op when the ride already has a real GPS-matched attempt for this segment`() = runTest {
        val attemptRepository = FakeSaveAttemptSegmentAttemptRepository(hasLocal = true)
        val useCase = SaveStravaSegmentEffortAttemptUseCase(
            FakeSaveAttemptSegmentRepository(listOf(segment)),
            FakeSaveAttemptStravaSegmentRepository(),
            FakeSaveAttemptRideRepository(ride),
            attemptRepository,
            MatchNewSegmentsToRidesUseCase(attemptRepository),
        )

        useCase(ride.id, effort, detail)

        assertTrue(attemptRepository.saved.isEmpty())
    }

    @Test
    fun `is a no-op when the detail has no track`() = runTest {
        val attemptRepository = FakeSaveAttemptSegmentAttemptRepository()
        val useCase = SaveStravaSegmentEffortAttemptUseCase(
            FakeSaveAttemptSegmentRepository(listOf(segment)),
            FakeSaveAttemptStravaSegmentRepository(),
            FakeSaveAttemptRideRepository(ride),
            attemptRepository,
            MatchNewSegmentsToRidesUseCase(attemptRepository),
        )

        useCase(ride.id, effort, detail.copy(track = emptyList()))

        assertTrue(attemptRepository.saved.isEmpty())
    }
}

private data class SavedStravaAttempt(
    val segmentId: Long,
    val rideId: Long,
    val startTime: Instant,
    val duration: Duration,
    val avgSpeedKmh: Double,
    val elevationGainMeters: Double,
    val avgPowerWatts: Double?,
    val effortExternalId: String,
)

private class FakeSaveAttemptSegmentRepository(initial: List<Segment>) : SegmentRepository {
    private val state = MutableStateFlow(initial)
    val savedExternalIds = mutableListOf<String>()
    var assignedId: Long = 100

    override fun observeSegments(): Flow<List<Segment>> = state

    override suspend fun saveSegments(segments: List<Segment>): List<Long> {
        savedExternalIds += segments.map { it.externalId }
        val saved = segments.map { it.copy(id = assignedId) }
        state.value = state.value + saved
        return saved.map { it.id }
    }

    override fun observeFilteredSegments(tag: String?, afterEpochMillis: Long?, beforeEpochMillis: Long?): Flow<List<Segment>> =
        throw UnsupportedOperationException("not used in this test")
}

private class FakeSaveAttemptStravaSegmentRepository(
    private val fetchResult: Result<Segment> = Result.failure(UnsupportedOperationException("not used in this test")),
) : StravaSegmentRepository {
    override suspend fun fetchStarredSegments(): Result<List<Segment>> =
        Result.failure(UnsupportedOperationException("not used in this test"))

    override suspend fun fetchSegment(segmentExternalId: String): Result<Segment> = fetchResult
}

private class FakeSaveAttemptRideRepository(private val ride: Ride?) : RideRepository {
    override fun observeRides(): Flow<List<Ride>> = MutableStateFlow(listOfNotNull(ride))
    override fun observeRide(rideId: Long): Flow<Ride?> = MutableStateFlow(ride)
    override fun observeHasTrack(rideId: Long): Flow<Boolean> = MutableStateFlow(false)
    override suspend fun saveRides(rides: List<Ride>): Int = 0
    override suspend fun saveRide(ride: Ride): Long? = null
    override suspend fun updateRide(rideId: Long, name: String, tag: String?, activityType: ActivityType) = Unit
    override suspend fun setTagForRides(rideIds: List<Long>, tag: String?) = Unit
    override suspend fun setActivityTypeForRides(rideIds: List<Long>, activityType: ActivityType) = Unit
    override fun observeAllTags(): Flow<List<String>> = MutableStateFlow(emptyList())
}

private class FakeSaveAttemptSegmentAttemptRepository(private val hasLocal: Boolean = false) : SegmentAttemptRepository {
    val saved = mutableListOf<SavedStravaAttempt>()
    val matchedSegmentIds = mutableListOf<Long>()

    override fun observeAttemptsForSegment(segmentId: Long): Flow<List<SegmentAttempt>> = MutableStateFlow(emptyList())
    override fun observeMatchesForRide(rideId: Long): Flow<List<RideSegmentMatch>> = MutableStateFlow(emptyList())
    override suspend fun trackPointsForAttempt(attemptId: Long): List<TrackPoint> = emptyList()
    override suspend fun matchRideAgainstAllSegments(rideId: Long): Int = 0

    override suspend fun matchSegmentAgainstAllRides(segmentId: Long): Int {
        matchedSegmentIds += segmentId
        return 0
    }

    override suspend fun saveStravaEffortAttempt(
        segmentId: Long,
        rideId: Long,
        startTime: Instant,
        duration: Duration,
        avgSpeedKmh: Double,
        elevationGainMeters: Double,
        avgPowerWatts: Double?,
        effortExternalId: String,
    ) {
        saved += SavedStravaAttempt(segmentId, rideId, startTime, duration, avgSpeedKmh, elevationGainMeters, avgPowerWatts, effortExternalId)
    }

    override suspend fun hasLocalAttempt(segmentId: Long, rideId: Long): Boolean = hasLocal
}
