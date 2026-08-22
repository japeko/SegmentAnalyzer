package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.Ride
import java.time.LocalDate

/** Fetches ride activities from the connected Garmin Connect account. */
interface GarminImportRepository {
    /**
     * The rider's cycling activities, newest first, optionally narrowed to [startDate]..[endDate]
     * (either or both may be null to leave that end of the range open). Requires an active connection.
     */
    suspend fun fetchRecentRides(
        limit: Int = 100,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
    ): Result<List<Ride>>
}

/** Thrown when there's no usable Garmin Connect session to import with. */
class GarminSessionExpiredException :
    Exception("Your Garmin Connect session has expired. Reconnect in Settings to import rides.")
