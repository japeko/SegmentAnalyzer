package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.StravaConnectionState
import com.segmentanalyzer.domain.repository.StravaAccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class StravaAccountUseCasesTest {

    @Test
    fun `observe use case reflects repository state`() = runTest {
        val repository = FakeStravaAccountRepository()
        val useCase = ObserveStravaConnectionStateUseCase(repository)

        assertEquals(StravaConnectionState.Disconnected, useCase().first())

        repository.state.value = StravaConnectionState.Connected("Jari K", Instant.EPOCH)

        assertEquals(StravaConnectionState.Connected("Jari K", Instant.EPOCH), useCase().first())
    }

    @Test
    fun `authorization url use case delegates to repository`() {
        val repository = FakeStravaAccountRepository()
        val useCase = GetStravaAuthorizationUrlUseCase(repository)

        assertEquals("https://www.strava.com/oauth/authorize?fake=true", useCase())
    }

    @Test
    fun `connect use case delegates the code to repository`() = runTest {
        val repository = FakeStravaAccountRepository()
        val useCase = ConnectStravaAccountUseCase(repository)

        val result = useCase("auth-code")

        assertTrue(result.isSuccess)
        assertEquals("auth-code", repository.lastCode)
    }

    @Test
    fun `disconnect use case delegates to repository`() = runTest {
        val repository = FakeStravaAccountRepository()
        repository.state.value = StravaConnectionState.Connected("Jari K", Instant.EPOCH)
        val useCase = DisconnectStravaAccountUseCase(repository)

        useCase()

        assertEquals(StravaConnectionState.Disconnected, repository.state.value)
    }
}

private class FakeStravaAccountRepository : StravaAccountRepository {
    val state = MutableStateFlow<StravaConnectionState>(StravaConnectionState.Disconnected)
    var lastCode: String? = null

    override fun observeConnectionState(): Flow<StravaConnectionState> = state

    override fun authorizationUrl(): String = "https://www.strava.com/oauth/authorize?fake=true"

    override suspend fun exchangeAuthorizationCode(code: String): Result<Unit> {
        lastCode = code
        state.value = StravaConnectionState.Connected("Jari K", Instant.EPOCH)
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        state.value = StravaConnectionState.Disconnected
    }
}
