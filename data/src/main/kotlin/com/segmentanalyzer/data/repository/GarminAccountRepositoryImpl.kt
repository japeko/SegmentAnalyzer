package com.segmentanalyzer.data.repository

import com.segmentanalyzer.common.DispatcherProvider
import com.segmentanalyzer.data.local.GarminSessionStore
import com.segmentanalyzer.data.remote.garmin.GarminSsoClient
import com.segmentanalyzer.data.remote.garmin.GarminSsoException
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

    override fun lastUsername(): String? = sessionStore.lastUsername()

    override fun signInUrl(): String = ssoClient.signinUrl()

    override fun isSignInComplete(url: String): Boolean = ssoClient.ticketFrom(url) != null

    override suspend fun completeSignIn(username: String, completionUrl: String): Result<Unit> =
        withContext(dispatcherProvider.io) {
            runCatching {
                val ticket = ssoClient.ticketFrom(completionUrl)
                    ?: throw GarminSsoException.Unexpected("no ticket in the sign-in completion URL")
                val service = ssoClient.serviceFrom(completionUrl)
                    ?: throw GarminSsoException.Unexpected("couldn't resolve the ticket's service URL")
                sessionStore.saveLastUsername(username)
                sessionStore.save(ssoClient.completeSession(username, ticket, service))
            }
        }

    override suspend fun disconnect() {
        withContext(dispatcherProvider.io) { sessionStore.clear() }
    }
}
