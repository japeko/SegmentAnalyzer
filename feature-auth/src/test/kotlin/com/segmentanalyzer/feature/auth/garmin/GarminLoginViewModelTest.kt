package com.segmentanalyzer.feature.auth.garmin

import app.cash.turbine.test
import com.segmentanalyzer.domain.model.GarminConnectionState
import com.segmentanalyzer.domain.repository.GarminAccountRepository
import com.segmentanalyzer.domain.usecase.ConnectGarminAccountUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GarminLoginViewModelTest {

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
    fun `successful connect updates state to connected`() = runTest(dispatcher) {
        val viewModel = GarminLoginViewModel(ConnectGarminAccountUseCase(FakeGarminAccountRepository()))

        viewModel.onUsernameChanged("rider@example.com")
        viewModel.onPasswordChanged("hunter2")

        viewModel.uiState.test {
            assertTrue(awaitItem().canSubmit)

            viewModel.onConnectClick()

            assertTrue(awaitItem().isLoading)
            val result = awaitItem()
            assertTrue(result.isConnected)
            assertNull(result.errorMessage)
        }
    }

    @Test
    fun `failed connect surfaces the error message`() = runTest(dispatcher) {
        val repository = FakeGarminAccountRepository(connectResult = Result.failure(IllegalStateException("bad credentials")))
        val viewModel = GarminLoginViewModel(ConnectGarminAccountUseCase(repository))

        viewModel.onUsernameChanged("rider@example.com")
        viewModel.onPasswordChanged("wrong")

        viewModel.uiState.test {
            assertTrue(awaitItem().canSubmit)

            viewModel.onConnectClick()

            assertTrue(awaitItem().isLoading)
            val result = awaitItem()
            assertEquals(false, result.isConnected)
            assertEquals("bad credentials", result.errorMessage)
        }
    }
}

private class FakeGarminAccountRepository(
    private val connectResult: Result<Unit> = Result.success(Unit),
) : GarminAccountRepository {
    private val state = MutableStateFlow<GarminConnectionState>(GarminConnectionState.Disconnected)

    override fun observeConnectionState(): Flow<GarminConnectionState> = state

    override suspend fun connect(username: String, password: String): Result<Unit> = connectResult

    override suspend fun disconnect() {
        state.value = GarminConnectionState.Disconnected
    }
}
