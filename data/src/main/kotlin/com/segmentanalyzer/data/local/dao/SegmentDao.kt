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

    /** Inserts segments, skipping ones whose [SegmentEntity.externalId] already exists (-1 per skip). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(segments: List<SegmentEntity>): List<Long>
}
