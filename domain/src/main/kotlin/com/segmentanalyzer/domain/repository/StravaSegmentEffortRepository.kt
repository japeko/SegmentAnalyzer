package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.model.StravaSegmentEffortDetail
import com.segmentanalyzer.domain.model.StravaSegmentEffortPoint
import kotlinx.coroutines.flow.Flow

/** Local cache of Strava segment effort data, fetched on demand via [StravaActivityRepository]. */
interface StravaSegmentEffortRepository {
    /** Cached efforts for [rideId], most recently fetched. Empty if never fetched (or none found). */
    fun observeEffortsForRide(rideId: Long): Flow<List<StravaSegmentEffort>>

    /** Replaces the cached efforts for [rideId] with [efforts]. */
    suspend fun replaceEffortsForRide(rideId: Long, efforts: List<StravaSegmentEffort>)

    /**
     * Saves [detail] for the effort with [effortExternalId] — the data used to compare this
     * effort against the same segment on a different ride. A no-op if that effort isn't cached
     * (e.g. its ride's effort list was refreshed and replaced it before this returned).
     */
    suspend fun saveEffortDetail(effortExternalId: String, detail: StravaSegmentEffortDetail)

    /** Replaces the cached GPS track for the effort with [effortExternalId] with [points]. */
    suspend fun saveEffortTrack(effortExternalId: String, points: List<StravaSegmentEffortPoint>)

    /** The cached GPS track for the effort with [effortExternalId], for the Segments page. */
    suspend fun trackForEffort(effortExternalId: String): List<StravaSegmentEffortPoint>
}
