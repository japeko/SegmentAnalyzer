package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.ActivityType
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

    /** Renames [rideId], sets its tag (null clears it) and its activity type. */
    suspend fun updateRide(rideId: Long, name: String, tag: String?, activityType: ActivityType)

    /** Sets (or, if [tag] is blank, clears) the same tag on every ride in [rideIds] at once. */
    suspend fun setTagForRides(rideIds: List<Long>, tag: String?)

    /** Sets the same activity type on every ride in [rideIds] at once. */
    suspend fun setActivityTypeForRides(rideIds: List<Long>, activityType: ActivityType)

    /** Deletes [rideId] and, via cascade, its segment attempts and any cached Strava effort data. */
    suspend fun deleteRide(rideId: Long)

    /** Every distinct, non-blank tag currently in use, for autocomplete when editing a ride. */
    fun observeAllTags(): Flow<List<String>>
}
