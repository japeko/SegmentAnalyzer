package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.Ride
import kotlinx.coroutines.flow.Flow

/** Read/write access to the locally stored ride history. All rides live on-device. */
interface RideRepository {
    /** All rides, most recent first. */
    fun observeRides(): Flow<List<Ride>>

    /** A single ride by id, or null if it doesn't exist. [Ride.track] is not populated. */
    fun observeRide(rideId: Long): Flow<Ride?>

    /** Whether [rideId] has a stored GPS track (only FIT/GPX-imported rides do). */
    fun observeHasTrack(rideId: Long): Flow<Boolean>

    /**
     * Saves imported rides, skipping ones already present (matched by [Ride.externalId]).
     * Returns how many were newly inserted.
     */
    suspend fun saveRides(rides: List<Ride>): Int

    /**
     * Saves a single ride and its track (if any). Returns the new row id, or null if it was a
     * duplicate — matched by [Ride.externalId] when present, otherwise by start time (local
     * FIT/GPX imports have no externalId).
     */
    suspend fun saveRide(ride: Ride): Long?
}
