package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.TrackPoint
import kotlinx.coroutines.flow.Flow

/** Matches locally-imported ride tracks against starred segments' start/end coordinates. */
interface SegmentAttemptRepository {
    fun observeAttemptsForSegment(segmentId: Long): Flow<List<SegmentAttempt>>

    /** The attempt's sub-track (entry..exit), distance re-based so the first point is 0. */
    suspend fun trackPointsForAttempt(attemptId: Long): List<TrackPoint>

    /** Matches [rideId]'s track against every known segment. Returns the number of new attempts. */
    suspend fun matchRideAgainstAllSegments(rideId: Long): Int

    /** Matches [segmentId] against every ride with a stored track. Returns the number of new attempts. */
    suspend fun matchSegmentAgainstAllRides(segmentId: Long): Int
}
