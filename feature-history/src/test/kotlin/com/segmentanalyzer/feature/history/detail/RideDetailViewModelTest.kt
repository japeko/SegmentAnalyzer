package com.segmentanalyzer.feature.history.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.RideSegmentMatch
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.model.StravaSegmentEffortDetail
import com.segmentanalyzer.domain.model.StravaSegmentEffortPoint
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.repository.StravaActivityRepository
import com.segmentanalyzer.domain.repository.StravaSegmentEffortRepository
import com.segmentanalyzer.domain.usecase.FetchStravaSegmentEffortDetailUseCase
import com.segmentanalyzer.domain.usecase.FetchStravaSegmentEffortsUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideHasTrackUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentMatchesForRideUseCase
import com.segmentanalyzer.domain.usecase.ObserveStravaSegmentEffortsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

private val ride = Ride(
    id = 1,
    name = "Tampere Pyöräily",
    activityType = ActivityType.GRAVEL,
    source = ActivitySource.FIT_FILE,
    startTime = Instant.parse("2026-08-17T06:00:00Z"),
    duration = Duration.ofSeconds(6_019),
    distanceMeters = 38_800.0,
    elevationGainMeters = 569.0,
    isPersonalBest = true,
    elevationProfile = listOf(0.1f, 0.5f, 1.0f, 0.3f),
    sourceFilePath = "content://imports/ride.fit",
)

private fun match(id: Long, name: String, isPersonalBest: Boolean) = RideSegmentMatch(
    attemptId = id,
    segmentId = id,
    segmentName = name,
    segmentDistanceMeters = 2_100.0,
    startTime = Instant.parse("2026-08-17T06:10:00Z"),
    duration = Duration.ofSeconds(200),
    avgSpeedKmh = 20.0,
    isPersonalBest = isPersonalBest,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RideDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `formats ride stats and matched segments for a ride with a track`() = runTest(dispatcher) {
        val matches = listOf(match(1, "Widow Creek Descent", isPersonalBest = true))
        val viewModel = viewModel(ride = ride, hasTrack = true, matches = matches)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals("Tampere Pyöräily", state.ride?.name)
        assertEquals(38.8, state.ride?.distanceKm)
        assertEquals(true, state.ride?.isPersonalBest)
        assertEquals(true, state.hasTrack)
        assertEquals(1, state.matchedSegments.size)
        assertEquals("Widow Creek Descent", state.matchedSegments.first().name)
        assertEquals(true, state.matchedSegments.first().isPersonalBest)
        collectJob.cancel()
    }

    @Test
    fun `a ride without a track has hasTrack false and no matches`() = runTest(dispatcher) {
        val viewModel = viewModel(ride = ride, hasTrack = false, matches = emptyList())

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.hasTrack)
        assertEquals(emptyList<MatchedSegmentItem>(), state.matchedSegments)
        collectJob.cancel()
    }

    @Test
    fun `an unknown ride id yields a null ride`() = runTest(dispatcher) {
        val viewModel = viewModel(ride = null, hasTrack = false, matches = emptyList())

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(null, state.ride)
        collectJob.cancel()
    }

    @Test
    fun `fetching Strava segments shows loading then the fetched efforts`() = runTest(dispatcher) {
        val efforts = listOf(
            StravaSegmentEffort(
                effortExternalId = "effort-1",
                segmentExternalId = "seg-1",
                segmentName = "Skyline Climb",
                elapsedTime = Duration.ofMinutes(4),
                distanceMeters = 1_200.0,
                komRank = null,
                prRank = 2,
            ),
        )
        val viewModel = viewModel(
            ride = ride,
            hasTrack = true,
            matches = emptyList(),
            stravaResult = Result.success(efforts),
        )

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(StravaEffortsUiState.Idle, awaitItem().stravaSegmentEfforts)

            viewModel.onFetchStravaSegmentsClick()

            assertEquals(StravaEffortsUiState.Loading, awaitItem().stravaSegmentEfforts)
            val loaded = awaitItem().stravaSegmentEfforts
            assertEquals(1, (loaded as StravaEffortsUiState.Loaded).efforts.size)
            assertEquals("Skyline Climb", loaded.efforts.first().segmentName)
            assertEquals(2, loaded.efforts.first().prRank)
        }
    }

    @Test
    fun `previously cached Strava efforts show immediately without needing a fetch`() = runTest(dispatcher) {
        val cached = listOf(
            StravaSegmentEffort(
                effortExternalId = "effort-2",
                segmentExternalId = "seg-2",
                segmentName = "Fireroad Descent",
                elapsedTime = Duration.ofMinutes(2),
                distanceMeters = 800.0,
                komRank = 4,
                prRank = null,
            ),
        )
        val viewModel = viewModel(ride = ride, hasTrack = true, matches = emptyList(), cachedEfforts = cached)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = (viewModel.uiState.value.stravaSegmentEfforts as StravaEffortsUiState.Loaded)
        assertEquals(1, state.efforts.size)
        assertEquals("Fireroad Descent", state.efforts.first().segmentName)
        assertEquals(4, state.efforts.first().komRank)
        collectJob.cancel()
    }

    @Test
    fun `a failed Strava fetch shows the error message`() = runTest(dispatcher) {
        val viewModel = viewModel(
            ride = ride,
            hasTrack = true,
            matches = emptyList(),
            stravaResult = Result.failure(IllegalStateException("Strava session expired")),
        )

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(StravaEffortsUiState.Idle, awaitItem().stravaSegmentEfforts)

            viewModel.onFetchStravaSegmentsClick()

            assertEquals(StravaEffortsUiState.Loading, awaitItem().stravaSegmentEfforts)
            assertEquals(
                StravaEffortsUiState.Error("Strava session expired"),
                awaitItem().stravaSegmentEfforts,
            )
        }
    }

    @Test
    fun `clicking a segment effort expands it and shows its detail`() = runTest(dispatcher) {
        val detail = StravaSegmentEffortDetail(
            avgSpeedKmh = 24.5,
            maxSpeedKmh = 38.0,
            elevationGainMeters = 12.0,
            avgWatts = 210.0,
            avgHeartRateBpm = 152.0,
            avgCadenceRpm = 78.0,
        )
        val viewModel = viewModel(
            ride = ride,
            hasTrack = true,
            matches = emptyList(),
            detailResult = Result.success(detail),
        )

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(null, awaitItem().expandedSegmentEffortDetail)

            viewModel.onStravaSegmentEffortClick(0, "effort-1")

            val loading = awaitItem().expandedSegmentEffortDetail
            assertEquals("effort-1", loading?.effortExternalId)
            assertEquals(StravaEffortDetailUiState.Loading, loading?.state)

            val loaded = awaitItem().expandedSegmentEffortDetail
            val loadedState = loaded?.state as StravaEffortDetailUiState.Loaded
            assertEquals("210 W", loadedState.detail.avgWattsLabel)
            assertEquals("152 bpm", loadedState.detail.avgHeartRateLabel)

            viewModel.onStravaSegmentEffortClick(0, "effort-1")
            assertEquals(null, awaitItem().expandedSegmentEffortDetail)
        }
    }

    @Test
    fun `clicking an effort with already-cached detail shows it immediately without fetching`() = runTest(dispatcher) {
        val cachedDetail = StravaSegmentEffortDetail(
            avgSpeedKmh = 19.0,
            maxSpeedKmh = 30.0,
            elevationGainMeters = 1.0,
            avgWatts = null,
            avgHeartRateBpm = 133.0,
            avgCadenceRpm = null,
        )
        val cached = listOf(
            StravaSegmentEffort(
                effortExternalId = "effort-1",
                segmentExternalId = "seg-1",
                segmentName = "Korde Ebike DH ränni 2",
                elapsedTime = Duration.ofSeconds(66),
                distanceMeters = 300.0,
                komRank = null,
                prRank = null,
                detail = cachedDetail,
            ),
        )
        val viewModel = viewModel(
            ride = ride,
            hasTrack = true,
            matches = emptyList(),
            cachedEfforts = cached,
            detailResult = Result.failure(IllegalStateException("should not be called")),
        )

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(null, awaitItem().expandedSegmentEffortDetail)

            viewModel.onStravaSegmentEffortClick(0, "effort-1")

            // Cache-first: goes straight to Loaded, never Loading, and never calls fetchStravaSegmentEffortDetail.
            val loaded = awaitItem().expandedSegmentEffortDetail?.state
            val loadedState = loaded as StravaEffortDetailUiState.Loaded
            assertEquals("133 bpm", loadedState.detail.avgHeartRateLabel)
            assertEquals(null, loadedState.detail.avgWattsLabel)
        }
    }

    @Test
    fun `a failed segment effort detail fetch shows the error message`() = runTest(dispatcher) {
        val viewModel = viewModel(
            ride = ride,
            hasTrack = true,
            matches = emptyList(),
            detailResult = Result.failure(IllegalStateException("Couldn't reach Strava")),
        )

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(null, awaitItem().expandedSegmentEffortDetail)

            viewModel.onStravaSegmentEffortClick(0, "effort-1")

            skipItems(1) // loading
            val errored = awaitItem().expandedSegmentEffortDetail?.state
            assertEquals(StravaEffortDetailUiState.Error("Couldn't reach Strava"), errored)
        }
    }

    private fun viewModel(
        ride: Ride?,
        hasTrack: Boolean,
        matches: List<RideSegmentMatch>,
        stravaResult: Result<List<StravaSegmentEffort>> = Result.success(emptyList()),
        cachedEfforts: List<StravaSegmentEffort> = emptyList(),
        detailResult: Result<StravaSegmentEffortDetail> = Result.failure(UnsupportedOperationException("not used")),
    ): RideDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("rideId" to 1L))
        val rideRepository = FakeRideDetailRideRepository(ride, hasTrack)
        val segmentAttemptRepository = FakeRideDetailSegmentAttemptRepository(matches)
        val stravaActivityRepository = FakeStravaActivityRepository(stravaResult, detailResult)
        val stravaEffortRepository = FakeStravaSegmentEffortRepository(mutableMapOf(1L to cachedEfforts))
        return RideDetailViewModel(
            savedStateHandle,
            ObserveRideUseCase(rideRepository),
            ObserveRideHasTrackUseCase(rideRepository),
            ObserveSegmentMatchesForRideUseCase(segmentAttemptRepository),
            ObserveStravaSegmentEffortsUseCase(stravaEffortRepository),
            FetchStravaSegmentEffortsUseCase(stravaActivityRepository, stravaEffortRepository),
            FetchStravaSegmentEffortDetailUseCase(stravaActivityRepository, stravaEffortRepository),
        )
    }
}

private class FakeRideDetailRideRepository(
    private val ride: Ride?,
    private val hasTrack: Boolean,
) : RideRepository {
    override fun observeRides(): Flow<List<Ride>> = MutableStateFlow(emptyList())
    override fun observeRide(rideId: Long): Flow<Ride?> = MutableStateFlow(ride)
    override fun observeHasTrack(rideId: Long): Flow<Boolean> = MutableStateFlow(hasTrack)
    override suspend fun saveRides(rides: List<Ride>): Int = 0
    override suspend fun saveRide(ride: Ride): Long? = null
}

private class FakeRideDetailSegmentAttemptRepository(
    private val matches: List<RideSegmentMatch>,
) : SegmentAttemptRepository {
    override fun observeAttemptsForSegment(segmentId: Long): Flow<List<SegmentAttempt>> = MutableStateFlow(emptyList())
    override fun observeMatchesForRide(rideId: Long): Flow<List<RideSegmentMatch>> = MutableStateFlow(matches)
    override suspend fun trackPointsForAttempt(attemptId: Long): List<TrackPoint> = emptyList()
    override suspend fun matchRideAgainstAllSegments(rideId: Long): Int = 0
    override suspend fun matchSegmentAgainstAllRides(segmentId: Long): Int = 0
}

private class FakeStravaActivityRepository(
    private val segmentEffortsResult: Result<List<StravaSegmentEffort>>,
    private val detailResult: Result<StravaSegmentEffortDetail>,
) : StravaActivityRepository {
    override suspend fun fetchSegmentEfforts(ride: Ride): Result<List<StravaSegmentEffort>> = segmentEffortsResult
    override suspend fun fetchEffortDetail(effortExternalId: String): Result<StravaSegmentEffortDetail> = detailResult
}

private class FakeStravaSegmentEffortRepository(
    initial: Map<Long, List<StravaSegmentEffort>> = emptyMap(),
) : StravaSegmentEffortRepository {
    private val state = MutableStateFlow(initial)

    override fun observeEffortsForRide(rideId: Long): Flow<List<StravaSegmentEffort>> =
        state.map { it[rideId].orEmpty() }

    override suspend fun replaceEffortsForRide(rideId: Long, efforts: List<StravaSegmentEffort>) {
        state.value = state.value + (rideId to efforts)
    }

    override suspend fun saveEffortDetail(effortExternalId: String, detail: StravaSegmentEffortDetail) {
        state.value = state.value.mapValues { (_, efforts) ->
            efforts.map { if (it.effortExternalId == effortExternalId) it.copy(detail = detail) else it }
        }
    }

    private val tracks = mutableMapOf<String, List<StravaSegmentEffortPoint>>()

    override suspend fun saveEffortTrack(effortExternalId: String, points: List<StravaSegmentEffortPoint>) {
        tracks[effortExternalId] = points
    }

    override suspend fun trackForEffort(effortExternalId: String): List<StravaSegmentEffortPoint> =
        tracks[effortExternalId].orEmpty()
}
