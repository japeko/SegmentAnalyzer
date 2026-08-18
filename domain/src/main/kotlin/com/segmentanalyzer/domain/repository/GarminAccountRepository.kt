package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.GarminConnectionState
import kotlinx.coroutines.flow.Flow

/** Manages the local session linking this device to a Garmin Connect account. */
interface GarminAccountRepository {
    /** Current connection state, updated as the account connects/disconnects. */
    fun observeConnectionState(): Flow<GarminConnectionState>

    /**
     * Logs in to Garmin Connect and stores the resulting session.
     * The password is used only for this call and is never persisted.
     */
    suspend fun connect(username: String, password: String): Result<Unit>

    /** Clears the locally stored Garmin Connect session. */
    suspend fun disconnect()
}
