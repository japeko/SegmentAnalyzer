package com.segmentanalyzer.feature.importer.garmin

import app.cash.turbine.test
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.GarminConnectionState
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.GarminAccountRepository
import com.segmentanalyzer.domain.repository.GarminImportRepository
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.usecase.ImportGarminRidesUseCase
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
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

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
    fun `successful import reports fetched and imported counts`() = runTest(dispatcher) {
        val viewModel = viewModel(connected = true, importRepository = FakeGarminImportRepository(Result.success(listOf(ride("1"), ride("2")))))

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(GarminImportUiState.Idle, awaitItem())

            viewModel.onImportClick()

            assertEquals(GarminImportUiState.Importing, awaitItem())
            assertEquals(GarminImportUiState.Result(fetchedCount = 2, importedCount = 1), awaitItem())
        }
    }

    @Test
    fun `failed import shows the error message`() = runTest(dispatcher) {
        val viewModel = viewModel(
            connected = true,
            importRepository = FakeGarminImportRepository(Result.failure(IllegalStateException("session expired"))),
        )

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(GarminImportUiState.Idle, awaitItem())

            viewModel.onImportClick()

            assertEquals(GarminImportUiState.Importing, awaitItem())
            assertEquals(GarminImportUiState.Error("session expired"), awaitItem())
        }
    }

    private fun viewModel(
        connected: Boolean,
        importRepository: FakeGarminImportRepository = FakeGarminImportRepository(Result.success(emptyList())),
    ): GarminImportViewModel {
        val accountState = if (connected) {
            GarminConnectionState.Connected("rider", Instant.EPOCH)
        } else {
            GarminConnectionState.Disconnected
        }
        return GarminImportViewModel(
            ObserveGarminConnectionStateUseCase(FakeGarminAccountRepository(accountState)),
            ImportGarminRidesUseCase(importRepository, FakeRideRepository(newRideCount = 1)),
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
    override suspend fun fetchRecentRides(limit: Int): Result<List<Ride>> = result
}

private class FakeRideRepository(private val newRideCount: Int) : RideRepository {
    override fun observeRides(): Flow<List<Ride>> = MutableStateFlow(emptyList())
    override fun observeRide(rideId: Long): Flow<Ride?> = MutableStateFlow(null)
    override fun observeHasTrack(rideId: Long): Flow<Boolean> = MutableStateFlow(false)
    override suspend fun saveRides(rides: List<Ride>): Int = newRideCount
    override suspend fun saveRide(ride: Ride): Long? = null
    override suspend fun updateRide(rideId: Long, name: String, tag: String?, activityType: ActivityType) = Unit
    override fun observeAllTags(): Flow<List<String>> = MutableStateFlow(emptyList())
}
