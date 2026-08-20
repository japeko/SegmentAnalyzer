package com.segmentanalyzer.feature.auth.garmin

import app.cash.turbine.test
import com.segmentanalyzer.domain.model.GarminConnectionState
import com.segmentanalyzer.domain.repository.GarminAccountRepository
import com.segmentanalyzer.domain.usecase.CompleteGarminSignInUseCase
import com.segmentanalyzer.domain.usecase.GetGarminSignInUrlUseCase
import com.segmentanalyzer.domain.usecase.GetLastGarminUsernameUseCase
import com.segmentanalyzer.domain.usecase.IsGarminSignInCompleteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
    fun `initial state carries the sign-in url and last username`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeGarminAccountRepository(lastUsername = "rider@example.com"))

        val state = viewModel.uiState.value
        assertEquals("rider@example.com", state.username)
        assertEquals("https://sso.garmin.com/sso/signin?fake=true", state.signInUrl)
        assertEquals(false, state.isConnected)
    }

    @Test
    fun `a non-completion url is ignored`() = runTest(dispatcher) {
        val repository = FakeGarminAccountRepository()
        val viewModel = viewModel(repository)

        viewModel.onWebViewUrlChanged("https://sso.garmin.com/sso/signin")

        assertEquals(false, viewModel.uiState.value.isExchangingToken)
        assertNull(repository.lastCompleteSignIn)
    }

    @Test
    fun `a completion url exchanges the ticket and connects`() = runTest(dispatcher) {
        val repository = FakeGarminAccountRepository()
        val viewModel = viewModel(repository)

        viewModel.uiState.test {
            assertEquals(false, awaitItem().isExchangingToken)

            viewModel.onWebViewUrlChanged("https://connect.garmin.com/modern?ticket=ST-1-abc")

            assertTrue(awaitItem().isExchangingToken)
            val connected = awaitItem()
            assertEquals(false, connected.isExchangingToken)
            assertTrue(connected.isConnected)
            assertEquals("" to "https://connect.garmin.com/modern?ticket=ST-1-abc", repository.lastCompleteSignIn)
        }
    }

    @Test
    fun `a failed exchange surfaces the error message`() = runTest(dispatcher) {
        val repository = FakeGarminAccountRepository(
            completeSignInResult = Result.failure(IllegalStateException("Garmin Connect login failed: HTTP 500")),
        )
        val viewModel = viewModel(repository)

        viewModel.uiState.test {
            assertEquals(false, awaitItem().isExchangingToken)

            viewModel.onWebViewUrlChanged("https://connect.garmin.com/modern?ticket=ST-1-abc")

            assertTrue(awaitItem().isExchangingToken)
            val failed = awaitItem()
            assertEquals(false, failed.isExchangingToken)
            assertEquals(false, failed.isConnected)
            assertEquals("Garmin Connect login failed: HTTP 500", failed.errorMessage)
        }
    }

    @Test
    fun `further url changes are ignored once already connected`() = runTest(dispatcher) {
        val repository = FakeGarminAccountRepository()
        val viewModel = viewModel(repository)

        viewModel.onWebViewUrlChanged("https://connect.garmin.com/modern?ticket=ST-1-abc")
        viewModel.onWebViewUrlChanged("https://connect.garmin.com/modern?ticket=ST-2-def")
        advanceUntilIdle()

        assertEquals(1, repository.completeSignInCallCount)
    }

    private fun viewModel(repository: GarminAccountRepository) = GarminLoginViewModel(
        CompleteGarminSignInUseCase(repository),
        IsGarminSignInCompleteUseCase(repository),
        GetGarminSignInUrlUseCase(repository),
        GetLastGarminUsernameUseCase(repository),
    )
}

private class FakeGarminAccountRepository(
    private val lastUsername: String? = null,
    private val completeSignInResult: Result<Unit> = Result.success(Unit),
) : GarminAccountRepository {
    private val state = MutableStateFlow<GarminConnectionState>(GarminConnectionState.Disconnected)
    var lastCompleteSignIn: Pair<String, String>? = null
        private set
    var completeSignInCallCount = 0
        private set

    override fun observeConnectionState(): Flow<GarminConnectionState> = state

    override fun lastUsername(): String? = lastUsername

    override fun signInUrl(): String = "https://sso.garmin.com/sso/signin?fake=true"

    override fun isSignInComplete(url: String): Boolean = url.contains("ticket=")

    override suspend fun completeSignIn(username: String, completionUrl: String): Result<Unit> {
        completeSignInCallCount++
        lastCompleteSignIn = username to completionUrl
        return completeSignInResult
    }

    override suspend fun disconnect() {
        state.value = GarminConnectionState.Disconnected
    }
}
