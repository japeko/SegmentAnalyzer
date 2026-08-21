package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.StravaSegmentEffort

/** Fetches Strava's own segment effort data for a ride, by matching it to a Strava activity. */
interface StravaActivityRepository {
    /**
     * Strava's segment efforts for the activity matching [ride]'s start time, or an empty list
     * if no matching Strava activity is found. Requires the `activity:read_all` OAuth scope.
     */
    suspend fun fetchSegmentEfforts(ride: Ride): Result<List<StravaSegmentEffort>>
}
