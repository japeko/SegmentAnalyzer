package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.StravaSegmentEffort
import kotlinx.coroutines.flow.Flow

/** Local cache of Strava segment effort data, fetched on demand via [StravaActivityRepository]. */
interface StravaSegmentEffortRepository {
    /** Cached efforts for [rideId], most recently fetched. Empty if never fetched (or none found). */
    fun observeEffortsForRide(rideId: Long): Flow<List<StravaSegmentEffort>>

    /** Replaces the cached efforts for [rideId] with [efforts]. */
    suspend fun replaceEffortsForRide(rideId: Long, efforts: List<StravaSegmentEffort>)
}
