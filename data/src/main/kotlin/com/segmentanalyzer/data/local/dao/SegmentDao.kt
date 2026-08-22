package com.segmentanalyzer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.segmentanalyzer.data.local.entity.SegmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SegmentDao {

    @Query("SELECT * FROM segments ORDER BY name ASC")
    fun observeAll(): Flow<List<SegmentEntity>>

    @Query("SELECT * FROM segments ORDER BY name ASC")
    suspend fun getAll(): List<SegmentEntity>

    @Query("SELECT * FROM segments WHERE id = :segmentId")
    suspend fun getById(segmentId: Long): SegmentEntity?

    /**
     * Segments with at least one attempt whose ride matches [tag] (its exact value) and whose
     * attempt's own start time falls within [afterEpochMillis]..[beforeEpochMillis) — any null
     * bound is unfiltered on that axis. A segment with zero matching attempts is excluded
     * entirely, including one with zero attempts at all.
     */
    @Query(
        """
        SELECT DISTINCT s.* FROM segments s
        INNER JOIN segment_attempts a ON a.segmentId = s.id
        INNER JOIN rides r ON r.id = a.rideId
        WHERE (:tag IS NULL OR r.tag = :tag)
          AND (:afterEpochMillis IS NULL OR a.startTimeEpochMillis >= :afterEpochMillis)
          AND (:beforeEpochMillis IS NULL OR a.startTimeEpochMillis < :beforeEpochMillis)
        ORDER BY s.name ASC
        """,
    )
    fun observeFiltered(tag: String?, afterEpochMillis: Long?, beforeEpochMillis: Long?): Flow<List<SegmentEntity>>

    /** Inserts segments, skipping ones whose [SegmentEntity.externalId] already exists (-1 per skip). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(segments: List<SegmentEntity>): List<Long>
}
