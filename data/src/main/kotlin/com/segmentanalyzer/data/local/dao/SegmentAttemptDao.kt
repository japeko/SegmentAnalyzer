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

    /** Atomically replaces the pseudo-attempt for [attempt]'s `stravaEffortExternalId` with [attempt]. */
    @Transaction
    suspend fun replaceStravaEffortAttempt(attempt: SegmentAttemptEntity) {
        deleteForStravaEffort(checkNotNull(attempt.stravaEffortExternalId))
        insert(attempt)
    }

    /**
     * True if [rideId] already has a real GPS-matched attempt (not Strava-derived) for
     * [segmentId] — see [com.segmentanalyzer.domain.usecase.SaveStravaSegmentEffortAttemptUseCase],
     * which skips creating a pseudo-attempt in that case: a Strava-derived row for the same ride's
     * pass through the same segment is a near-duplicate of the real one (same lap, very similar
     * time/track), which just confuses the comparison rather than adding anything.
     */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM segment_attempts WHERE segmentId = :segmentId AND rideId = :rideId AND stravaEffortExternalId IS NULL)",
    )
    suspend fun hasLocalAttempt(segmentId: Long, rideId: Long): Boolean

    /** Removes Strava-derived pseudo-attempts for (segmentId, rideId) now that a real one exists — see [hasLocalAttempt]. */
    @Query("DELETE FROM segment_attempts WHERE segmentId = :segmentId AND rideId = :rideId AND stravaEffortExternalId IS NOT NULL")
    suspend fun deleteStravaAttemptsForRideSegment(segmentId: Long, rideId: Long)

    /**
     * Self-heals rows saved before [hasLocalAttempt] existed to guard against them: any
     * Strava-derived attempt sharing a (segmentId, rideId) with a real GPS-matched one is a
     * near-duplicate of it and gets removed, same as [deleteStravaAttemptsForRideSegment].
     */
    @Query(
        """
        DELETE FROM segment_attempts WHERE stravaEffortExternalId IS NOT NULL AND EXISTS (
            SELECT 1 FROM segment_attempts b
            WHERE b.segmentId = segment_attempts.segmentId AND b.rideId = segment_attempts.rideId AND b.stravaEffortExternalId IS NULL
        )
        """,
    )
    suspend fun deleteRedundantStravaAttempts()
}
