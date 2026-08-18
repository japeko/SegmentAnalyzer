package com.segmentanalyzer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.segmentanalyzer.data.local.entity.RideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {

    @Query("SELECT * FROM rides ORDER BY startTimeEpochMillis DESC")
    fun observeAll(): Flow<List<RideEntity>>

    @Query("SELECT COUNT(*) FROM rides")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rides: List<RideEntity>)

    /** Inserts rides, skipping ones whose [RideEntity.externalId] already exists (-1 per skip). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(rides: List<RideEntity>): List<Long>
}
