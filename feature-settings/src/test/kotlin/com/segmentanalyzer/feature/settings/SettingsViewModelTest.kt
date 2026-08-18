package com.segmentanalyzer.feature.settings

import app.cash.turbine.test
import com.segmentanalyzer.domain.model.GarminConnectResult
import com.segmentanalyzer.domain.model.GarminConnectionState
import com.segmentanalyzer.domain.repository.GarminAccountRepository
import com.segmentanalyzer.domain.usecase.DisconnectGarminAccountUseCase
import com.segmentanalyzer.domain.usecase.ObserveGarminConnectionStateUseCase
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
    fun `disconnect requires confirmation before clearing the session`() = runTest(dispatcher) {
        val repository = FakeGarminAccountRepository(
            initialState = GarminConnectionState.Connected("rider", Instant.EPOCH),
        )
        val viewModel = SettingsViewModel(
            ObserveGarminConnectionStateUseCase(repository),
            DisconnectGarminAccountUseCase(repository),
        )

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
    fun `dismissing the confirmation does not disconnect`() = runTest(dispatcher) {
        val repository = FakeGarminAccountRepository(
            initialState = GarminConnectionState.Connected("rider", Instant.EPOCH),
        )
        val viewModel = SettingsViewModel(
            ObserveGarminConnectionStateUseCase(repository),
            DisconnectGarminAccountUseCase(repository),
        )

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
