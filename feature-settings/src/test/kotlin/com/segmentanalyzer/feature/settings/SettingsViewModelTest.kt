package com.segmentanalyzer.feature.settings

import app.cash.turbine.test
import com.segmentanalyzer.domain.model.GarminConnectResult
import com.segmentanalyzer.domain.model.GarminConnectionState
import com.segmentanalyzer.domain.model.StravaConnectionState
import com.segmentanalyzer.domain.repository.GarminAccountRepository
import com.segmentanalyzer.domain.repository.StravaAccountRepository
import com.segmentanalyzer.domain.usecase.DisconnectGarminAccountUseCase
import com.segmentanalyzer.domain.usecase.DisconnectStravaAccountUseCase
import com.segmentanalyzer.domain.usecase.GetStravaAuthorizationUrlUseCase
import com.segmentanalyzer.domain.usecase.ObserveGarminConnectionStateUseCase
import com.segmentanalyzer.domain.usecase.ObserveStravaConnectionStateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

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
    fun `disconnect requires confirmation before clearing the garmin session`() = runTest(dispatcher) {
        val garminRepository = FakeGarminAccountRepository(
            initialState = GarminConnectionState.Connected("rider", Instant.EPOCH),
        )
        val viewModel = viewModel(garminRepository = garminRepository)

        viewModel.uiState.test {
            skipItems(1) // stateIn's initialValue, emitted before the combine picks up the real state
            val initial = awaitItem()
            assertEquals(GarminConnectionState.Connected("rider", Instant.EPOCH), initial.garminConnectionState)
            assertFalse(initial.showDisconnectGarminConfirmation)

            viewModel.onDisconnectGarminClick()
            assertTrue(awaitItem().showDisconnectGarminConfirmation)

            viewModel.onConfirmDisconnectGarmin()
            val confirmed = awaitItem()
            assertFalse(confirmed.showDisconnectGarminConfirmation)

            val disconnected = awaitItem()
            assertEquals(GarminConnectionState.Disconnected, disconnected.garminConnectionState)
        }
    }

    @Test
    fun `dismissing the garmin confirmation does not disconnect`() = runTest(dispatcher) {
        val garminRepository = FakeGarminAccountRepository(
            initialState = GarminConnectionState.Connected("rider", Instant.EPOCH),
        )
        val viewModel = viewModel(garminRepository = garminRepository)

        viewModel.uiState.test {
            skipItems(1) // stateIn's initialValue, emitted before the combine picks up the real state
            awaitItem()
            viewModel.onDisconnectGarminClick()
            awaitItem()

            viewModel.onDismissDisconnectGarmin()
            val dismissed = awaitItem()

            assertFalse(dismissed.showDisconnectGarminConfirmation)
            assertEquals(GarminConnectionState.Connected("rider", Instant.EPOCH), dismissed.garminConnectionState)
        }
    }

    @Test
    fun `disconnect requires confirmation before clearing the strava session`() = runTest(dispatcher) {
        val stravaRepository = FakeStravaAccountRepository(
            initialState = StravaConnectionState.Connected("Jari K", Instant.EPOCH),
        )
        val viewModel = viewModel(stravaRepository = stravaRepository)

        viewModel.uiState.test {
            skipItems(1) // stateIn's initialValue, emitted before the combine picks up the real state
            val initial = awaitItem()
            assertEquals(StravaConnectionState.Connected("Jari K", Instant.EPOCH), initial.stravaConnectionState)
            assertFalse(initial.showDisconnectStravaConfirmation)

            viewModel.onDisconnectStravaClick()
            assertTrue(awaitItem().showDisconnectStravaConfirmation)

            viewModel.onConfirmDisconnectStrava()
            val confirmed = awaitItem()
            assertFalse(confirmed.showDisconnectStravaConfirmation)

            val disconnected = awaitItem()
            assertEquals(StravaConnectionState.Disconnected, disconnected.stravaConnectionState)
        }
    }

    @Test
    fun `exposes the strava authorization url from the use case`() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertEquals("https://www.strava.com/oauth/authorize?fake=true", viewModel.stravaAuthorizationUrl)
    }

    private fun viewModel(
        garminRepository: GarminAccountRepository = FakeGarminAccountRepository(),
        stravaRepository: StravaAccountRepository = FakeStravaAccountRepository(),
    ) = SettingsViewModel(
        ObserveGarminConnectionStateUseCase(garminRepository),
        DisconnectGarminAccountUseCase(garminRepository),
        ObserveStravaConnectionStateUseCase(stravaRepository),
        DisconnectStravaAccountUseCase(stravaRepository),
        GetStravaAuthorizationUrlUseCase(stravaRepository),
    )
}

private class FakeGarminAccountRepository(
    initialState: GarminConnectionState = GarminConnectionState.Disconnected,
) : GarminAccountRepository {
    private val state = MutableStateFlow(initialState)

    override fun observeConnectionState(): Flow<GarminConnectionState> = state

    override fun lastUsername(): String? = null

    override suspend fun connect(username: String, password: String): Result<GarminConnectResult> =
        Result.success(GarminConnectResult.Connected)

    override suspend fun submitMfaCode(code: String): Result<Unit> = Result.success(Unit)

    override suspend fun disconnect() {
        state.value = GarminConnectionState.Disconnected
    }
}

private class FakeStravaAccountRepository(
    initialState: StravaConnectionState = StravaConnectionState.Disconnected,
) : StravaAccountRepository {
    private val state = MutableStateFlow(initialState)

    override fun observeConnectionState(): Flow<StravaConnectionState> = state

    override fun authorizationUrl(): String = "https://www.strava.com/oauth/authorize?fake=true"

    override suspend fun exchangeAuthorizationCode(code: String): Result<Unit> = Result.success(Unit)

    override suspend fun disconnect() {
        state.value = StravaConnectionState.Disconnected
    }
}
