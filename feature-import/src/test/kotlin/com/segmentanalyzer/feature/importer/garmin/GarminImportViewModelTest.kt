package com.segmentanalyzer.feature.importer.garmin

import app.cash.turbine.test
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.GarminConnectionState
import com.segmentanalyzer.domain.model.RideSegmentMatch
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.GarminAccountRepository
import com.segmentanalyzer.domain.repository.GarminImportRepository
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.usecase.FetchGarminRidesUseCase
import com.segmentanalyzer.domain.usecase.ImportGarminRidesUseCase
import com.segmentanalyzer.domain.usecase.MatchNewRideToSegmentsUseCase
import com.segmentanalyzer.domain.usecase.ObserveGarminConnectionStateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class GarminImportViewModelTest {

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
    fun `not connected shows the not-connected state`() = runTest(dispatcher) {
        val viewModel = viewModel(connected = false)

        // Here the derived state happens to equal stateIn's initialValue, so there's no second
        // emission to await (StateFlow only emits on change) — collect to start the sharing,
        // advance, then assert the settled value directly instead of via a turbine emission.
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(GarminImportUiState.NotConnected, viewModel.uiState.value)
        collectJob.cancel()
    }

    @Test
    fun `browsing rides shows all of them pre-selected`() = runTest(dispatcher) {
        val viewModel = viewModel(connected = true, importRepository = FakeGarminImportRepository(Result.success(listOf(ride("1"), ride("2")))))

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(GarminImportUiState.Idle(dateFrom = null, dateTo = null), awaitItem())

            viewModel.onBrowseRidesClick()

            assertEquals(GarminImportUiState.FetchingRides, awaitItem())
            val selecting = awaitItem() as GarminImportUiState.SelectingRides
            assertEquals(2, selecting.candidates.size)
            assertEquals(setOf("1", "2"), selecting.selectedExternalIds)
        }
    }

    @Test
    fun `picking a date range passes it through to the fetch and onto the picker screen`() = runTest(dispatcher) {
        val importRepository = FakeGarminImportRepository(Result.success(listOf(ride("1"))))
        val viewModel = viewModel(connected = true, importRepository = importRepository)
        val from = LocalDate.of(2026, 6, 1)
        val to = LocalDate.of(2026, 6, 30)

        viewModel.uiState.test {
            skipItems(1)
            awaitItem() // Idle, no dates yet

            viewModel.onDateFromSelected(from)
            assertEquals(GarminImportUiState.Idle(dateFrom = from, dateTo = null), awaitItem())

            viewModel.onDateToSelected(to)
            assertEquals(GarminImportUiState.Idle(dateFrom = from, dateTo = to), awaitItem())

            viewModel.onBrowseRidesClick()
            awaitItem() // FetchingRides
            val selecting = awaitItem() as GarminImportUiState.SelectingRides
            assertEquals(from, selecting.dateFrom)
            assertEquals(to, selecting.dateTo)
        }
        assertEquals(from, importRepository.lastStartDate)
        assertEquals(to, importRepository.lastEndDate)
    }

    @Test
    fun `browsing more rides after a result returns to idle with the same date range`() = runTest(dispatcher) {
        val viewModel = viewModel(connected = true, importRepository = FakeGarminImportRepository(Result.success(listOf(ride("1")))))
        val from = LocalDate.of(2026, 6, 1)

        viewModel.uiState.test {
            skipItems(1)
            awaitItem() // Idle, no dates yet
            viewModel.onDateFromSelected(from)
            awaitItem() // Idle, from set

            viewModel.onBrowseRidesClick()
            awaitItem() // FetchingRides
            awaitItem() // SelectingRides
            viewModel.onImportSelectedClick()
            awaitItem() // Importing
            awaitItem() // Result

            viewModel.onBackToIdleClick()
            val idle = awaitItem() as GarminImportUiState.Idle
            assertEquals(from, idle.dateFrom)
            assertNull(idle.dateTo)
        }
    }

    @Test
    fun `deselecting a ride excludes it from the import`() = runTest(dispatcher) {
        val rideRepository = FakeRideRepository(newRideCount = 1)
        val viewModel = viewModel(
            connected = true,
            importRepository = FakeGarminImportRepository(Result.success(listOf(ride("1"), ride("2")))),
            rideRepository = rideRepository,
        )

        viewModel.uiState.test {
            skipItems(1)
            awaitItem() // Idle
            viewModel.onBrowseRidesClick()
            awaitItem() // FetchingRides
            awaitItem() // SelectingRides, both selected

            viewModel.onRideToggled("2")
            val afterToggle = awaitItem() as GarminImportUiState.SelectingRides
            assertEquals(setOf("1"), afterToggle.selectedExternalIds)

            viewModel.onImportSelectedClick()
            assertEquals(GarminImportUiState.Importing, awaitItem())
            assertEquals(GarminImportUiState.Result(selectedCount = 1, importedCount = 1), awaitItem())
        }
        assertEquals(listOf("1"), rideRepository.lastSavedExternalIds)
    }

    @Test
    fun `select all toggles between everything and nothing`() = runTest(dispatcher) {
        val viewModel = viewModel(connected = true, importRepository = FakeGarminImportRepository(Result.success(listOf(ride("1"), ride("2")))))

        viewModel.uiState.test {
            skipItems(1)
            awaitItem() // Idle
            viewModel.onBrowseRidesClick()
            awaitItem() // FetchingRides
            awaitItem() // SelectingRides, both selected

            viewModel.onSelectAllToggled()
            assertEquals(emptySet<String>(), (awaitItem() as GarminImportUiState.SelectingRides).selectedExternalIds)

            viewModel.onSelectAllToggled()
            assertEquals(setOf("1", "2"), (awaitItem() as GarminImportUiState.SelectingRides).selectedExternalIds)
        }
    }

    @Test
    fun `a fetch failure shows the error message`() = runTest(dispatcher) {
        val viewModel = viewModel(
            connected = true,
            importRepository = FakeGarminImportRepository(Result.failure(IllegalStateException("session expired"))),
        )

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(GarminImportUiState.Idle(dateFrom = null, dateTo = null), awaitItem())

            viewModel.onBrowseRidesClick()

            assertEquals(GarminImportUiState.FetchingRides, awaitItem())
            assertEquals(GarminImportUiState.Error("session expired"), awaitItem())
        }
    }

    private fun viewModel(
        connected: Boolean,
        importRepository: FakeGarminImportRepository = FakeGarminImportRepository(Result.success(emptyList())),
        rideRepository: FakeRideRepository = FakeRideRepository(newRideCount = 1),
    ): GarminImportViewModel {
        val accountState = if (connected) {
            GarminConnectionState.Connected("rider", Instant.EPOCH)
        } else {
            GarminConnectionState.Disconnected
        }
        return GarminImportViewModel(
            ObserveGarminConnectionStateUseCase(FakeGarminAccountRepository(accountState)),
            FetchGarminRidesUseCase(importRepository),
            ImportGarminRidesUseCase(
                importRepository,
                rideRepository,
                MatchNewRideToSegmentsUseCase(FakeSegmentAttemptRepository()),
            ),
        )
    }
}

private fun ride(externalId: String) = Ride(
    id = 0,
    name = "Ride $externalId",
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

private class FakeGarminAccountRepository(initialState: GarminConnectionState) : GarminAccountRepository {
    private val state = MutableStateFlow(initialState)
    override fun observeConnectionState(): Flow<GarminConnectionState> = state

    override fun lastUsername(): String? = null
    override fun signInUrl(): String = "https://sso.garmin.com/sso/signin?fake=true"
    override fun isSignInComplete(url: String): Boolean = false
    override suspend fun completeSignIn(username: String, completionUrl: String): Result<Unit> = Result.success(Unit)

    override suspend fun disconnect() {
        state.value = GarminConnectionState.Disconnected
    }
}

private class FakeGarminImportRepository(private val result: Result<List<Ride>>) : GarminImportRepository {
    var lastStartDate: LocalDate? = null
        private set
    var lastEndDate: LocalDate? = null
        private set

    override suspend fun fetchRecentRides(limit: Int, startDate: LocalDate?, endDate: LocalDate?): Result<List<Ride>> {
        lastStartDate = startDate
        lastEndDate = endDate
        return result
    }

    override suspend fun fetchTrack(externalId: String): List<TrackPoint> = emptyList()
}

/** Saves up to [newRideCount] rides (by call order) with an incrementing id; any beyond that are treated as duplicates (null). */
private class FakeRideRepository(private val newRideCount: Int) : RideRepository {
    var lastSavedExternalIds: List<String> = emptyList()
        private set
    private var nextId = 1L
    private var savedCount = 0

    override fun observeRides(): Flow<List<Ride>> = MutableStateFlow(emptyList())
    override fun observeRide(rideId: Long): Flow<Ride?> = MutableStateFlow(null)
    override fun observeHasTrack(rideId: Long): Flow<Boolean> = MutableStateFlow(false)
    override suspend fun saveRides(rides: List<Ride>): Int = 0

    override suspend fun saveRide(ride: Ride): Long? {
        lastSavedExternalIds = lastSavedExternalIds + listOfNotNull(ride.externalId)
        if (savedCount >= newRideCount) return null
        savedCount++
        return nextId++
    }

    override suspend fun updateRide(rideId: Long, name: String, tag: String?, activityType: ActivityType) = Unit
    override suspend fun setTagForRides(rideIds: List<Long>, tag: String?) = Unit
    override suspend fun setActivityTypeForRides(rideIds: List<Long>, activityType: ActivityType) = Unit
    override fun observeAllTags(): Flow<List<String>> = MutableStateFlow(emptyList())
}

private class FakeSegmentAttemptRepository : SegmentAttemptRepository {
    override fun observeAttemptsForSegment(segmentId: Long): Flow<List<SegmentAttempt>> = MutableStateFlow(emptyList())
    override fun observeMatchesForRide(rideId: Long): Flow<List<RideSegmentMatch>> = MutableStateFlow(emptyList())
    override suspend fun trackPointsForAttempt(attemptId: Long): List<TrackPoint> = emptyList()
    override suspend fun matchRideAgainstAllSegments(rideId: Long): Int = 0
    override suspend fun matchSegmentAgainstAllRides(segmentId: Long): Int = 0

    override suspend fun saveStravaEffortAttempt(
        segmentId: Long,
        rideId: Long,
        startTime: java.time.Instant,
        duration: java.time.Duration,
        avgSpeedKmh: Double,
        elevationGainMeters: Double,
        avgPowerWatts: Double?,
        effortExternalId: String,
    ) = Unit

    override suspend fun hasLocalAttempt(segmentId: Long, rideId: Long): Boolean = false
}
