package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.Ride

/** Fetches ride activities from the connected Garmin Connect account. */
interface GarminImportRepository {
    /** The rider's most recent cycling activities, newest first. Requires an active connection. */
    suspend fun fetchRecentRides(limit: Int = 20): Result<List<Ride>>
}

/** Thrown when there's no usable Garmin Connect session to import with. */
class GarminSessionExpiredException :
    Exception("Your Garmin Connect session has expired. Reconnect in Settings to import rides.")
