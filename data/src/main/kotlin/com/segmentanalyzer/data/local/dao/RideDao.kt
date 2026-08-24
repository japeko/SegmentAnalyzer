package com.segmentanalyzer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.segmentanalyzer.data.local.entity.RideEntity
import com.segmentanalyzer.domain.model.ActivityType
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {

    @Query("SELECT * FROM rides ORDER BY startTimeEpochMillis DESC")
    fun observeAll(): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE id = :rideId")
    fun rideById(rideId: Long): Flow<RideEntity?>

    @Query("SELECT COUNT(*) FROM rides")
    suspend fun count(): Int

    /**
     * Whether a ride starting at [startTimeEpochMillis] is already stored. Used to de-duplicate
     * FIT/GPX imports, which have no [RideEntity.externalId] to rely on for the unique index
     * below (SQLite treats every NULL as distinct, so that index never catches them).
     */
    @Query("SELECT EXISTS(SELECT 1 FROM rides WHERE startTimeEpochMillis = :startTimeEpochMillis)")
    suspend fun existsWithStartTime(startTimeEpochMillis: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rides: List<RideEntity>)

    /** Inserts rides, skipping ones whose [RideEntity.externalId] already exists (-1 per skip). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(rides: List<RideEntity>): List<Long>

    @Query("UPDATE rides SET name = :name, tag = :tag, activityType = :activityType WHERE id = :rideId")
    suspend fun updateDetails(rideId: Long, name: String, tag: String?, activityType: ActivityType)

    @Query("UPDATE rides SET tag = :tag WHERE id IN (:rideIds)")
    suspend fun setTagForRides(rideIds: List<Long>, tag: String?)

    @Query("UPDATE rides SET activityType = :activityType WHERE id IN (:rideIds)")
    suspend fun setActivityTypeForRides(rideIds: List<Long>, activityType: ActivityType)

    @Query("SELECT DISTINCT tag FROM rides WHERE tag IS NOT NULL AND tag != '' ORDER BY tag ASC")
    fun observeDistinctTags(): Flow<List<String>>
}
