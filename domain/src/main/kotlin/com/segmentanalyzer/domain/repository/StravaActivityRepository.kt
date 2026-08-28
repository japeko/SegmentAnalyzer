package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.model.StravaSegmentEffortDetail

/** Fetches Strava's own segment effort data for a ride, by matching it to a Strava activity. */
interface StravaActivityRepository {
    /**
     * Strava's segment efforts for the activity matching [ride]'s start time, or an empty list
     * if the matched activity crossed no segments. Fails with [StravaActivityNotFoundException]
     * if no matching Strava activity is found at all. Requires the `activity:read_all` OAuth scope.
     */
    suspend fun fetchSegmentEfforts(ride: Ride): Result<List<StravaSegmentEffort>>

    /** Pace/power/HR/cadence summary for one specific effort, from its point-by-point streams. */
    suspend fun fetchEffortDetail(effortExternalId: String): Result<StravaSegmentEffortDetail>
}

/**
 * Thrown when no Strava activity's start time falls near [Ride.startTime]. Strava's
 * `athlete/activities` list endpoint silently omits activities set to "Only You" privacy, even
 * with `activity:read_all` granted, so this is also the symptom of a private ride on Strava.
 */
class StravaActivityNotFoundException : Exception(
    "No matching Strava activity found for this ride. If it's set to \"Only You\" on Strava, " +
        "the API won't return it — try \"Followers\" or public visibility.",
)
