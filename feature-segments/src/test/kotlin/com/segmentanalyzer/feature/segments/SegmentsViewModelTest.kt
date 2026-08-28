package com.segmentanalyzer.feature.segments

import app.cash.turbine.test
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.Segment
import com.segmentanalyzer.domain.model.StravaConnectionState
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.repository.SegmentRepository
import com.segmentanalyzer.domain.repository.StravaAccountRepository
import com.segmentanalyzer.domain.repository.StravaSegmentRepository
import com.segmentanalyzer.domain.usecase.MatchNewSegmentsToRidesUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideTagsUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentsUseCase
import com.segmentanalyzer.domain.usecase.ObserveStravaConnectionStateUseCase
import com.segmentanalyzer.domain.usecase.SyncStravaSegmentsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SegmentsViewModelTest {

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
    fun `not connected shows the not-connected status`() = runTest(dispatcher) {
        val viewModel = viewModel(connected = false)

        // Derived state happens to equal stateIn's initialValue here, so there's no guaranteed
        // second emission to await — collect, advance, then assert the settled value directly.
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(StravaSyncStatus.NotConnected, viewModel.uiState.value.syncStatus)
        collectJob.cancel()
    }

    @Test
    fun `successful sync reports fetched and synced counts`() = runTest(dispatcher) {
        val viewModel = viewModel(
            connected = true,
            segmentRepository = FakeSegmentRepository(),
            stravaSegmentRepository = FakeStravaSegmentRepository(Result.success(listOf(segment("1"), segment("2")))),
        )

        viewModel.uiState.test {
            skipItems(1) // stateIn's initialValue, emitted before the combine picks up the real state
            assertEquals(StravaSyncStatus.Idle, awaitItem().syncStatus)

            viewModel.onSyncClick()

            assertEquals(StravaSyncStatus.Syncing, awaitItem().syncStatus)
            assertEquals(StravaSyncStatus.Result(fetchedCount = 2, syncedCount = 1), awaitItem().syncStatus)
        }
    }

    @Test
    fun `failed sync shows the error message`() = runTest(dispatcher) {
        val viewModel = viewModel(
            connected = true,
            segmentRepository = FakeSegmentRepository(),
            stravaSegmentRepository = FakeStravaSegmentRepository(Result.failure(IllegalStateException("session expired"))),
        )

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(StravaSyncStatus.Idle, awaitItem().syncStatus)

            viewModel.onSyncClick()

            assertEquals(StravaSyncStatus.Syncing, awaitItem().syncStatus)
            assertEquals(StravaSyncStatus.Error("session expired"), awaitItem().syncStatus)
        }
    }

    @Test
    fun `sync result auto-dismisses back to Idle after 10 seconds`() = runTest(dispatcher) {
        val viewModel = viewModel(
            connected = true,
            segmentRepository = FakeSegmentRepository(),
            stravaSegmentRepository = FakeStravaSegmentRepository(Result.success(listOf(segment("1")))),
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // runCurrent(), not advanceUntilIdle(): the latter fast-forwards through the 10s delay
        // too, since it keeps advancing the virtual clock until nothing at all is scheduled.
        viewModel.onSyncClick()
        runCurrent()
        assertEquals(StravaSyncStatus.Result(fetchedCount = 1, syncedCount = 1), viewModel.uiState.value.syncStatus)

        advanceTimeBy(10_001)
        runCurrent()
        assertEquals(StravaSyncStatus.Idle, viewModel.uiState.value.syncStatus)
        collectJob.cancel()
    }

    @Test
    fun `a second sync's equal-valued result isn't clipped early by the first sync's own timer`() = runTest(dispatcher) {
        val viewModel = viewModel(
            connected = true,
            segmentRepository = FakeSegmentRepository(),
            stravaSegmentRepository = FakeStravaSegmentRepository(Result.success(listOf(segment("1")))),
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSyncClick()
        runCurrent()
        assertEquals(StravaSyncStatus.Result(fetchedCount = 1, syncedCount = 1), viewModel.uiState.value.syncStatus)

        advanceTimeBy(9_000)
        viewModel.onSyncClick() // a second sync, producing an equal-by-value Result, before the first's 10s timer fires
        runCurrent()

        // Past the first sync's original 10s mark (9_000 + 1_500), but well short of the second's own.
        advanceTimeBy(1_500)
        runCurrent()
        assertEquals(StravaSyncStatus.Result(fetchedCount = 1, syncedCount = 1), viewModel.uiState.value.syncStatus)
        collectJob.cancel()
    }

    @Test
    fun `shows unfiltered segments and available tags by default`() = runTest(dispatcher) {
        val unfiltered = listOf(segment("1"), segment("2"))
        val viewModel = viewModel(
            connected = false,
            segmentRepository = FakeSegmentRepository(unfilteredSegments = unfiltered),
            rideTags = listOf("Race", "Training"),
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        val state = viewModel.uiState.value
        collectJob.cancel()

        assertEquals(unfiltered, state.segments)
        assertEquals(listOf("Race", "Training"), state.availableTags)
        assertEquals(false, state.isFilterActive)
    }

    @Test
    fun `selecting a tag switches to the filtered segment list`() = runTest(dispatcher) {
        val filtered = listOf(segment("1"))
        val segmentRepository = FakeSegmentRepository(
            unfilteredSegments = listOf(segment("1"), segment("2")),
            filteredSegments = filtered,
        )
        val viewModel = viewModel(connected = false, segmentRepository = segmentRepository, rideTags = listOf("Race"))
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onTagSelected("Race")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        collectJob.cancel()
        assertEquals(filtered, state.segments)
        assertEquals("Race", state.selectedTag)
        assertTrue(state.isFilterActive)
        assertEquals("Race", segmentRepository.lastFilterCall?.first)
    }

    @Test
    fun `clearing filters reverts to the unfiltered segment list`() = runTest(dispatcher) {
        val unfiltered = listOf(segment("1"), segment("2"))
        val segmentRepository = FakeSegmentRepository(unfilteredSegments = unfiltered, filteredSegments = listOf(segment("1")))
        val viewModel = viewModel(connected = false, segmentRepository = segmentRepository)
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onTagSelected("Race")
        advanceUntilIdle()
        viewModel.onClearFiltersClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        collectJob.cancel()
        assertEquals(unfiltered, state.segments)
        assertNull(state.selectedTag)
        assertEquals(false, state.isFilterActive)
    }

    @Test
    fun `date range is converted to an exclusive end-of-day upper bound`() = runTest(dispatcher) {
        val segmentRepository = FakeSegmentRepository(filteredSegments = listOf(segment("1")))
        val viewModel = viewModel(connected = false, segmentRepository = segmentRepository)
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDateFromSelected(LocalDate.of(2026, 8, 1))
        viewModel.onDateToSelected(LocalDate.of(2026, 8, 22))
        advanceUntilIdle()
        collectJob.cancel()

        val (_, after, before) = segmentRepository.lastFilterCall!!
        assertTrue(after!! < before!!)
        // "to" is inclusive of the whole day, so the upper bound is the start of the next day.
        assertEquals(LocalDate.of(2026, 8, 23), Instant.ofEpochMilli(before).atZone(java.time.ZoneId.systemDefault()).toLocalDate())
    }

    @Test
    fun `filter sheet visibility toggles`() = runTest(dispatcher) {
        val viewModel = viewModel(connected = false)
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isFilterSheetVisible)

        viewModel.onFilterClick()
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.isFilterSheetVisible)

        viewModel.onDismissFilterSheet()
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isFilterSheetVisible)
        collectJob.cancel()
    }

    private fun viewModel(
        connected: Boolean,
        segmentRepository: FakeSegmentRepository = FakeSegmentRepository(),
        stravaSegmentRepository: FakeStravaSegmentRepository = FakeStravaSegmentRepository(Result.success(emptyList())),
        rideTags: List<String> = emptyList(),
    ): SegmentsViewModel {
        val accountState = if (connected) {
            StravaConnectionState.Connected("Jari K", Instant.EPOCH)
        } else {
            StravaConnectionState.Disconnected
        }
        return SegmentsViewModel(
            ObserveSegmentsUseCase(segmentRepository),
            ObserveStravaConnectionStateUseCase(FakeStravaAccountRepository(accountState)),
            ObserveRideTagsUseCase(FakeSegmentsVmRideRepository(rideTags)),
            SyncStravaSegmentsUseCase(
                stravaSegmentRepository,
                FakeSegmentRepository(newCount = 1),
                MatchNewSegmentsToRidesUseCase(FakeSegmentsVmSegmentAttemptRepository()),
            ),
        )
    }
}

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

private class FakeStravaAccountRepository(initialState: StravaConnectionState) : StravaAccountRepository {
    private val state = MutableStateFlow(initialState)
    override fun observeConnectionState(): Flow<StravaConnectionState> = state
    override fun authorizationUrl(): String = "https://www.strava.com/oauth/authorize?fake=true"
    override suspend fun exchangeAuthorizationCode(code: String): Result<Unit> = Result.success(Unit)
    override suspend fun disconnect() {
        state.value = StravaConnectionState.Disconnected
    }
}

private class FakeStravaSegmentRepository(private val result: Result<List<Segment>>) : StravaSegmentRepository {
    override suspend fun fetchStarredSegments(): Result<List<Segment>> = result
    override suspend fun fetchSegment(segmentExternalId: String): Result<Segment> =
        Result.failure(UnsupportedOperationException("not used in this test"))
    override suspend fun isSegmentStarred(segmentExternalId: String): Result<Boolean> =
        Result.failure(UnsupportedOperationException("not used in this test"))
    override suspend fun setSegmentStarred(segmentExternalId: String, starred: Boolean): Result<Unit> =
        Result.failure(UnsupportedOperationException("not used in this test"))
}

private class FakeSegmentRepository(
    private val newCount: Int = 0,
    private val unfilteredSegments: List<Segment> = emptyList(),
    private val filteredSegments: List<Segment> = emptyList(),
) : SegmentRepository {
    var lastFilterCall: Triple<String?, Long?, Long?>? = null
        private set

    override fun observeSegments(): Flow<List<Segment>> = MutableStateFlow(unfilteredSegments)
    override suspend fun saveSegments(segments: List<Segment>): List<Long> = (1..newCount).map { it.toLong() }

    override fun observeFilteredSegments(tag: String?, afterEpochMillis: Long?, beforeEpochMillis: Long?): Flow<List<Segment>> {
        lastFilterCall = Triple(tag, afterEpochMillis, beforeEpochMillis)
        return MutableStateFlow(filteredSegments)
    }
}

private class FakeSegmentsVmRideRepository(private val tags: List<String>) : RideRepository {
    override fun observeRides(): Flow<List<Ride>> = MutableStateFlow(emptyList())
    override fun observeRide(rideId: Long): Flow<Ride?> = MutableStateFlow(null)
    override fun observeHasTrack(rideId: Long): Flow<Boolean> = MutableStateFlow(false)
    override suspend fun saveRides(rides: List<Ride>): Int = 0
    override suspend fun saveRide(ride: Ride): Long? = null
    override suspend fun updateRide(rideId: Long, name: String, tag: String?, activityType: ActivityType) = Unit
    override suspend fun setTagForRides(rideIds: List<Long>, tag: String?) = Unit
    override suspend fun setActivityTypeForRides(rideIds: List<Long>, activityType: ActivityType) = Unit
    override suspend fun deleteRide(rideId: Long) = Unit
    override fun observeAllTags(): Flow<List<String>> = MutableStateFlow(tags)
}

private class FakeSegmentsVmSegmentAttemptRepository : SegmentAttemptRepository {
    override fun observeAttemptsForSegment(segmentId: Long): Flow<List<SegmentAttempt>> = MutableStateFlow(emptyList())
    override fun observeMatchesForRide(rideId: Long) = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.RideSegmentMatch>())
    override fun observeImportedStravaEffortIds(rideId: Long) = MutableStateFlow(emptySet<String>())
    override fun observeRecords() = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.SegmentRecord>())
    override suspend fun trackPointsForAttempt(attemptId: Long): List<TrackPoint> = emptyList()
    override suspend fun matchRideAgainstAllSegments(rideId: Long): Int = 0
    override suspend fun matchSegmentAgainstAllRides(segmentId: Long): Int = 0
    override suspend fun saveStravaEffortAttempt(
        segmentId: Long, rideId: Long, startTime: java.time.Instant, duration: java.time.Duration,
        avgSpeedKmh: Double, elevationGainMeters: Double, avgPowerWatts: Double?, effortExternalId: String,
    ) = Unit
}
