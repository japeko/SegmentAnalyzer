package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.StravaConnectionState
import kotlinx.coroutines.flow.Flow

/** Manages the local session linking this device to a Strava account. */
interface StravaAccountRepository {
    /** Current connection state, updated as the account connects/disconnects. */
    fun observeConnectionState(): Flow<StravaConnectionState>

    /** The URL to open (in a browser/Custom Tab) to start Strava's OAuth consent flow. */
    fun authorizationUrl(): String

    /** Exchanges the authorization code from the OAuth redirect for a stored session. */
    suspend fun exchangeAuthorizationCode(code: String): Result<Unit>

    /** Clears the locally stored Strava session. */
    suspend fun disconnect()
}
