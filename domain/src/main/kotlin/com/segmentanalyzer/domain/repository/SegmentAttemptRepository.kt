package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.RideSegmentMatch
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.SegmentRecord
import com.segmentanalyzer.domain.model.TrackPoint
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant

/** Matches locally-imported ride tracks against starred segments' start/end coordinates. */
interface SegmentAttemptRepository {
    fun observeAttemptsForSegment(segmentId: Long): Flow<List<SegmentAttempt>>

    /** Segments [rideId] passed through, chronological. */
    fun observeMatchesForRide(rideId: Long): Flow<List<RideSegmentMatch>>

    /** External ids of every Strava segment effort already saved as an attempt for [rideId]. */
    fun observeImportedStravaEffortIds(rideId: Long): Flow<Set<String>>

    /** The current fastest attempt for every segment that has at least one, most recent first. */
    fun observeRecords(): Flow<List<SegmentRecord>>

    /**
     * The attempt's sub-track, distance re-based so the first point is 0 — real GPS points for a
     * locally-matched attempt, or the persisted Strava effort track (no elevation/sensors) for
     * one saved via [saveStravaEffortAttempt].
     */
    suspend fun trackPointsForAttempt(attemptId: Long): List<TrackPoint>

    /** Matches [rideId]'s track against every known segment. Returns the number of new attempts. */
    suspend fun matchRideAgainstAllSegments(rideId: Long): Int

    /** Matches [segmentId] against every ride with a stored track. Returns the number of new attempts. */
    suspend fun matchSegmentAgainstAllRides(segmentId: Long): Int

    /**
     * Saves (or replaces) a pseudo-attempt derived from a Strava segment effort, so it can be
     * compared via the same Compare Rides flow as locally GPS-matched attempts. Its track is read
     * live from the already-persisted Strava effort points at comparison time (see
     * [trackPointsForAttempt]) — nothing is duplicated here.
     */
    suspend fun saveStravaEffortAttempt(
        segmentId: Long,
        rideId: Long,
        startTime: Instant,
        duration: Duration,
        avgSpeedKmh: Double,
        elevationGainMeters: Double,
        avgPowerWatts: Double?,
        effortExternalId: String,
    )
}
