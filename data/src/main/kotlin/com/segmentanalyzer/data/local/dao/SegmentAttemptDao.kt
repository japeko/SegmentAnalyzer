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
}
