package com.segmentanalyzer.feature.auth.garmin

import app.cash.turbine.test
import com.segmentanalyzer.domain.model.GarminConnectResult
import com.segmentanalyzer.domain.model.GarminConnectionState
import com.segmentanalyzer.domain.repository.GarminAccountRepository
import com.segmentanalyzer.domain.usecase.ConnectGarminAccountUseCase
import com.segmentanalyzer.domain.usecase.GetLastGarminUsernameUseCase
import com.segmentanalyzer.domain.usecase.SubmitGarminMfaCodeUseCase
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
        val viewModel = viewModel(FakeGarminAccountRepository())

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
        val viewModel = viewModel(repository)

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

    @Test
    fun `MFA-required response moves to the code step, then submitting it connects`() = runTest(dispatcher) {
        val repository = FakeGarminAccountRepository(
            connectResult = Result.success(GarminConnectResult.MfaRequired),
            mfaResult = Result.success(Unit),
        )
        val viewModel = viewModel(repository)

        viewModel.onUsernameChanged("rider@example.com")
        viewModel.onPasswordChanged("hunter2")

        viewModel.uiState.test {
            assertTrue(awaitItem().canSubmit)

            viewModel.onConnectClick()

            assertTrue(awaitItem().isLoading)
            val awaitingMfa = awaitItem()
            assertEquals(GarminLoginStep.MfaCode, awaitingMfa.step)
            assertEquals(false, awaitingMfa.isConnected)

            viewModel.onMfaCodeChanged("123456")
            awaitItem() // mfaCode field update

            viewModel.onSubmitMfaCodeClick()

            assertTrue(awaitItem().isLoading)
            val connected = awaitItem()
            assertTrue(connected.isConnected)
            assertEquals("123456", repository.lastMfaCode)
        }
    }
}

private fun viewModel(repository: GarminAccountRepository) = GarminLoginViewModel(
    ConnectGarminAccountUseCase(repository),
    SubmitGarminMfaCodeUseCase(repository),
    GetLastGarminUsernameUseCase(repository),
)

private class FakeGarminAccountRepository(
    private val connectResult: Result<GarminConnectResult> = Result.success(GarminConnectResult.Connected),
    private val mfaResult: Result<Unit> = Result.success(Unit),
) : GarminAccountRepository {
    private val state = MutableStateFlow<GarminConnectionState>(GarminConnectionState.Disconnected)
    var lastMfaCode: String? = null
        private set

    override fun observeConnectionState(): Flow<GarminConnectionState> = state

    override fun lastUsername(): String? = null

    override suspend fun connect(username: String, password: String): Result<GarminConnectResult> = connectResult

    override suspend fun submitMfaCode(code: String): Result<Unit> {
        lastMfaCode = code
        return mfaResult
    }

    override suspend fun disconnect() {
        state.value = GarminConnectionState.Disconnected
    }
}
