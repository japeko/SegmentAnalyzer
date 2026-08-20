package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.GarminConnectionState
import com.segmentanalyzer.domain.repository.GarminAccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GarminAccountUseCasesTest {

    @Test
    fun `observe use case reflects repository state`() = runTest {
        val repository = FakeGarminAccountRepository()
        val useCase = ObserveGarminConnectionStateUseCase(repository)

        assertEquals(GarminConnectionState.Disconnected, useCase().first())

        repository.state.value = GarminConnectionState.Connected("rider", Instant.EPOCH)

        assertEquals(GarminConnectionState.Connected("rider", Instant.EPOCH), useCase().first())
    }

    @Test
    fun `sign-in url use case delegates to repository`() = runTest {
        val repository = FakeGarminAccountRepository()
        val useCase = GetGarminSignInUrlUseCase(repository)

        assertEquals("https://sso.garmin.com/sso/signin?fake=true", useCase())
    }

    @Test
    fun `sign-in complete use case delegates to repository`() = runTest {
        val repository = FakeGarminAccountRepository()
        val useCase = IsGarminSignInCompleteUseCase(repository)

        assertFalse(useCase("https://connect.garmin.com/modern"))
        assertTrue(useCase("https://connect.garmin.com/modern?ticket=ST-1-abc"))
    }

    @Test
    fun `complete sign-in use case delegates the completion url to repository`() = runTest {
        val repository = FakeGarminAccountRepository()
        val useCase = CompleteGarminSignInUseCase(repository)

        val result = useCase("rider@example.com", "https://connect.garmin.com/modern?ticket=ST-1-abc")

        assertTrue(result.isSuccess)
        assertEquals("rider@example.com" to "https://connect.garmin.com/modern?ticket=ST-1-abc", repository.lastCompleteSignIn)
    }

    @Test
    fun `disconnect use case delegates to repository`() = runTest {
        val repository = FakeGarminAccountRepository()
        repository.state.value = GarminConnectionState.Connected("rider", Instant.EPOCH)
        val useCase = DisconnectGarminAccountUseCase(repository)

        useCase()

        assertEquals(GarminConnectionState.Disconnected, repository.state.value)
    }
}

private class FakeGarminAccountRepository : GarminAccountRepository {
    val state = MutableStateFlow<GarminConnectionState>(GarminConnectionState.Disconnected)
    var lastCompleteSignIn: Pair<String, String>? = null

    override fun observeConnectionState(): Flow<GarminConnectionState> = state

    override fun lastUsername(): String? = null

    override fun signInUrl(): String = "https://sso.garmin.com/sso/signin?fake=true"

    override fun isSignInComplete(url: String): Boolean = url.contains("ticket=")

    override suspend fun completeSignIn(username: String, completionUrl: String): Result<Unit> {
        lastCompleteSignIn = username to completionUrl
        state.value = GarminConnectionState.Connected(username, Instant.EPOCH)
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        state.value = GarminConnectionState.Disconnected
    }
}
