package com.segmentanalyzer.data.repository

import com.segmentanalyzer.common.DispatcherProvider
import com.segmentanalyzer.data.local.GarminSessionStore
import com.segmentanalyzer.data.remote.garmin.GarminSsoClient
import com.segmentanalyzer.domain.model.GarminConnectionState
import com.segmentanalyzer.domain.repository.GarminAccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class GarminAccountRepositoryImpl @Inject constructor(
    private val ssoClient: GarminSsoClient,
    private val sessionStore: GarminSessionStore,
    private val dispatcherProvider: DispatcherProvider,
) : GarminAccountRepository {

    override fun observeConnectionState(): Flow<GarminConnectionState> = sessionStore.state

    override suspend fun connect(username: String, password: String): Result<Unit> =
        withContext(dispatcherProvider.io) {
            runCatching { sessionStore.save(ssoClient.login(username, password)) }
        }

    override suspend fun disconnect() {
        withContext(dispatcherProvider.io) { sessionStore.clear() }
    }
}
