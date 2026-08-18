package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.Ride
import kotlinx.coroutines.flow.Flow

/** Read/write access to the locally stored ride history. All rides live on-device. */
interface RideRepository {
    /** All rides, most recent first. */
    fun observeRides(): Flow<List<Ride>>

    /**
     * Saves imported rides, skipping ones already present (matched by [Ride.externalId]).
     * Returns how many were newly inserted.
     */
    suspend fun saveRides(rides: List<Ride>): Int

    /** Saves a single ride and its track (if any). Returns the new row id, or null if it was a duplicate. */
    suspend fun saveRide(ride: Ride): Long?
}
