package com.segmentanalyzer.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.segmentanalyzer.data.local.entity.SegmentAttemptEntity
import com.segmentanalyzer.domain.model.ActivitySource
import kotlinx.coroutines.flow.Flow

data class SegmentAttemptWithRide(
    @Embedded val attempt: SegmentAttemptEntity,
    val rideName: String,
    val rideSource: ActivitySource,
)

data class SegmentAttemptWithSegment(
    @Embedded val attempt: SegmentAttemptEntity,
    val segmentName: String,
    val segmentDistanceMeters: Double,
    val isPersonalBest: Boolean,
)

data class SegmentAttemptRecordRow(
    @Embedded val attempt: SegmentAttemptEntity,
    val segmentName: String,
    val segmentDistanceMeters: Double,
    val rideName: String,
    val rideSource: ActivitySource,
)

@Dao
interface SegmentAttemptDao {

    /** Inserts attempts, skipping ones whose (segmentId, rideId) pair already exists (-1 per skip). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(attempts: List<SegmentAttemptEntity>): List<Long>

    @Query(
        """
        SELECT a.*, r.name AS rideName, r.source AS rideSource
        FROM segment_attempts a
        JOIN rides r ON r.id = a.rideId
        WHERE a.segmentId = :segmentId
        ORDER BY a.startTimeEpochMillis ASC
        """,
    )
    fun observeForSegment(segmentId: Long): Flow<List<SegmentAttemptWithRide>>

    @Query(
        """
        SELECT a.*, s.name AS segmentName, s.distanceMeters AS segmentDistanceMeters,
          (a.durationMillis = (SELECT MIN(durationMillis) FROM segment_attempts WHERE segmentId = a.segmentId)) AS isPersonalBest
        FROM segment_attempts a
        JOIN segments s ON s.id = a.segmentId
        WHERE a.rideId = :rideId
        ORDER BY a.startTimeEpochMillis ASC
        """,
    )
    fun observeForRide(rideId: Long): Flow<List<SegmentAttemptWithSegment>>

    @Query("SELECT stravaEffortExternalId FROM segment_attempts WHERE rideId = :rideId AND stravaEffortExternalId IS NOT NULL")
    fun observeImportedStravaEffortIds(rideId: Long): Flow<List<String>>

    @Query("SELECT * FROM segment_attempts WHERE id = :attemptId")
    suspend fun attemptById(attemptId: Long): SegmentAttemptEntity?

    /**
     * The single fastest attempt per segment — its current record. Ties (identical
     * [SegmentAttemptEntity.durationMillis]) resolve to whichever happened first, so a segment
     * never contributes more than one row.
     */
    @Query(
        """
        SELECT a.*, s.name AS segmentName, s.distanceMeters AS segmentDistanceMeters, r.name AS rideName, r.source AS rideSource
        FROM segment_attempts a
        JOIN segments s ON s.id = a.segmentId
        JOIN rides r ON r.id = a.rideId
        WHERE a.id = (
            SELECT b.id FROM segment_attempts b
            WHERE b.segmentId = a.segmentId
            ORDER BY b.durationMillis ASC, b.startTimeEpochMillis ASC
            LIMIT 1
        )
        """,
    )
    fun observeRecords(): Flow<List<SegmentAttemptRecordRow>>

    @Insert
    suspend fun insert(attempt: SegmentAttemptEntity): Long

    @Query("DELETE FROM segment_attempts WHERE stravaEffortExternalId = :effortExternalId")
    suspend fun deleteForStravaEffort(effortExternalId: String)

    /**
     * Atomically replaces the pseudo-attempt for [attempt]'s `stravaEffortExternalId` with
     * [attempt], and clears out any real GPS-matched attempts for the same (segmentId, rideId) —
     * Strava's own effort detection is the more trustworthy source once it exists for a ride's
     * pass through a segment: naive point-matching against a locally-stored GPS track can miss a
     * lap entirely or mistime a crossing by a few seconds, in a way Strava's own effort data does
     * not.
     */
    @Transaction
    suspend fun replaceStravaEffortAttempt(attempt: SegmentAttemptEntity) {
        deleteForStravaEffort(checkNotNull(attempt.stravaEffortExternalId))
        deleteLocalAttemptsForRideSegment(attempt.segmentId, attempt.rideId)
        insert(attempt)
    }

    /**
     * True if [rideId] already has a Strava-derived attempt for [segmentId] — see
     * [com.segmentanalyzer.data.repository.SegmentAttemptRepositoryImpl.matchRideAgainstAllSegments]/
     * [com.segmentanalyzer.data.repository.SegmentAttemptRepositoryImpl.matchSegmentAgainstAllRides],
     * which skip inserting a real GPS-matched attempt in that case: see [replaceStravaEffortAttempt].
     */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM segment_attempts WHERE segmentId = :segmentId AND rideId = :rideId AND stravaEffortExternalId IS NOT NULL)",
    )
    suspend fun hasStravaAttempt(segmentId: Long, rideId: Long): Boolean

    /** Removes real GPS-matched attempts for (segmentId, rideId) now that Strava data exists for it — see [replaceStravaEffortAttempt]. */
    @Query("DELETE FROM segment_attempts WHERE segmentId = :segmentId AND rideId = :rideId AND stravaEffortExternalId IS NULL")
    suspend fun deleteLocalAttemptsForRideSegment(segmentId: Long, rideId: Long)

    /**
     * Self-heals rows saved before this Strava-wins precedence existed: any real GPS-matched
     * attempt sharing a (segmentId, rideId) with a Strava-derived one is superseded by it and
     * gets removed, same as [deleteLocalAttemptsForRideSegment].
     */
    @Query(
        """
        DELETE FROM segment_attempts WHERE stravaEffortExternalId IS NULL AND EXISTS (
            SELECT 1 FROM segment_attempts b
            WHERE b.segmentId = segment_attempts.segmentId AND b.rideId = segment_attempts.rideId AND b.stravaEffortExternalId IS NOT NULL
        )
        """,
    )
    suspend fun deleteRedundantLocalAttempts()
}
