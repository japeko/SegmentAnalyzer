package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.GarminConnectResult
import com.segmentanalyzer.domain.model.GarminConnectionState
import kotlinx.coroutines.flow.Flow

/** Manages the local session linking this device to a Garmin Connect account. */
interface GarminAccountRepository {
    /** Current connection state, updated as the account connects/disconnects. */
    fun observeConnectionState(): Flow<GarminConnectionState>

    /** The username from the last connect attempt (success or not), to pre-fill the login form. Never the password. */
    fun lastUsername(): String?

    /**
     * Logs in to Garmin Connect and, unless multi-factor auth is required, stores the
     * resulting session. The password is used only for this call and is never persisted.
     */
    suspend fun connect(username: String, password: String): Result<GarminConnectResult>

    /**
     * Completes a login that returned [GarminConnectResult.MfaRequired], using the code from
     * the rider's authenticator app or email, and stores the resulting session.
     */
    suspend fun submitMfaCode(code: String): Result<Unit>

    /** Clears the locally stored Garmin Connect session. */
    suspend fun disconnect()
}
