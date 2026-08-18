package com.segmentanalyzer.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.segmentanalyzer.data.local.entity.SegmentAttemptEntity
import com.segmentanalyzer.domain.model.ActivitySource
import kotlinx.coroutines.flow.Flow

data class SegmentAttemptWithRide(
    @Embedded val attempt: SegmentAttemptEntity,
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

    @Query("SELECT * FROM segment_attempts WHERE id = :attemptId")
    suspend fun attemptById(attemptId: Long): SegmentAttemptEntity?
}
