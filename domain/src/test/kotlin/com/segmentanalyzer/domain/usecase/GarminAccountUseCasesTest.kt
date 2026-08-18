package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.GarminConnectionState
import com.segmentanalyzer.domain.repository.GarminAccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    fun `connect use case delegates credentials to repository`() = runTest {
        val repository = FakeGarminAccountRepository()
        val useCase = ConnectGarminAccountUseCase(repository)

        val result = useCase("rider@example.com", "hunter2")

        assertTrue(result.isSuccess)
        assertEquals("rider@example.com" to "hunter2", repository.lastConnectAttempt)
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
    var lastConnectAttempt: Pair<String, String>? = null

    override fun observeConnectionState(): Flow<GarminConnectionState> = state

    override suspend fun connect(username: String, password: String): Result<Unit> {
        lastConnectAttempt = username to password
        state.value = GarminConnectionState.Connected(username, Instant.EPOCH)
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        state.value = GarminConnectionState.Disconnected
    }
}
