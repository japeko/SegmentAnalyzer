package com.segmentanalyzer.feature.history.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.RideSegmentMatch
import com.segmentanalyzer.domain.model.Segment
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.model.StravaSegmentEffortHistoryEntry
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.repository.StravaActivityRepository
import com.segmentanalyzer.domain.repository.StravaSegmentEffortRepository
import com.segmentanalyzer.domain.repository.StravaSegmentRepository
import com.segmentanalyzer.domain.usecase.FetchStravaSegmentEffortHistoryUseCase
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
    fun `clicking a segment effort expands it and shows its history`() = runTest(dispatcher) {
        val history = listOf(
            StravaSegmentEffortHistoryEntry(
                startTime = Instant.parse("2026-08-10T06:00:00Z"),
                elapsedTime = Duration.ofMinutes(4).plusSeconds(30),
                distanceMeters = 1_200.0,
                komRank = null,
                prRank = 1,
            ),
        )
        val viewModel = viewModel(
            ride = ride,
            hasTrack = true,
            matches = emptyList(),
            historyResult = Result.success(history),
        )

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(null, awaitItem().expandedSegmentEffortHistory)

            viewModel.onStravaSegmentEffortClick(0, "seg-1")

            val loading = awaitItem().expandedSegmentEffortHistory
            assertEquals("seg-1", loading?.segmentExternalId)
            assertEquals(StravaEffortHistoryUiState.Loading, loading?.state)

            val loaded = awaitItem().expandedSegmentEffortHistory
            val loadedState = loaded?.state as StravaEffortHistoryUiState.Loaded
            assertEquals(1, loadedState.entries.size)
            assertEquals(1, loadedState.entries.first().prRank)

            viewModel.onStravaSegmentEffortClick(0, "seg-1")
            assertEquals(null, awaitItem().expandedSegmentEffortHistory)
        }
    }

    @Test
    fun `a failed segment history fetch shows the error message`() = runTest(dispatcher) {
        val viewModel = viewModel(
            ride = ride,
            hasTrack = true,
            matches = emptyList(),
            historyResult = Result.failure(IllegalStateException("Couldn't reach Strava")),
        )

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(null, awaitItem().expandedSegmentEffortHistory)

            viewModel.onStravaSegmentEffortClick(0, "seg-1")

            skipItems(1) // loading
            val errored = awaitItem().expandedSegmentEffortHistory?.state
            assertEquals(StravaEffortHistoryUiState.Error("Couldn't reach Strava"), errored)
        }
    }

    private fun viewModel(
        ride: Ride?,
        hasTrack: Boolean,
        matches: List<RideSegmentMatch>,
        stravaResult: Result<List<StravaSegmentEffort>> = Result.success(emptyList()),
        cachedEfforts: List<StravaSegmentEffort> = emptyList(),
        historyResult: Result<List<StravaSegmentEffortHistoryEntry>> = Result.success(emptyList()),
    ): RideDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("rideId" to 1L))
        val rideRepository = FakeRideDetailRideRepository(ride, hasTrack)
        val segmentAttemptRepository = FakeRideDetailSegmentAttemptRepository(matches)
        val stravaActivityRepository = FakeStravaActivityRepository(stravaResult)
        val stravaEffortRepository = FakeStravaSegmentEffortRepository(mutableMapOf(1L to cachedEfforts))
        val stravaSegmentRepository = FakeStravaSegmentRepository(historyResult)
        return RideDetailViewModel(
            savedStateHandle,
            ObserveRideUseCase(rideRepository),
            ObserveRideHasTrackUseCase(rideRepository),
            ObserveSegmentMatchesForRideUseCase(segmentAttemptRepository),
            ObserveStravaSegmentEffortsUseCase(stravaEffortRepository),
            FetchStravaSegmentEffortsUseCase(stravaActivityRepository, stravaEffortRepository),
            FetchStravaSegmentEffortHistoryUseCase(stravaSegmentRepository),
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
    private val result: Result<List<StravaSegmentEffort>>,
) : StravaActivityRepository {
    override suspend fun fetchSegmentEfforts(ride: Ride): Result<List<StravaSegmentEffort>> = result
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
}

private class FakeStravaSegmentRepository(
    private val historyResult: Result<List<StravaSegmentEffortHistoryEntry>>,
) : StravaSegmentRepository {
    override suspend fun fetchStarredSegments(): Result<List<Segment>> = Result.success(emptyList())
    override suspend fun fetchEffortHistory(segmentExternalId: String): Result<List<StravaSegmentEffortHistoryEntry>> =
        historyResult
}
