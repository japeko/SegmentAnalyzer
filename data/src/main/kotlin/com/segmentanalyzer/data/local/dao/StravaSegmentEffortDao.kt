package com.segmentanalyzer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.segmentanalyzer.data.local.entity.StravaSegmentEffortEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StravaSegmentEffortDao {

    @Query("SELECT * FROM strava_segment_efforts WHERE rideId = :rideId")
    fun observeForRide(rideId: Long): Flow<List<StravaSegmentEffortEntity>>

    @Insert
    suspend fun insertAll(efforts: List<StravaSegmentEffortEntity>)

    @Query("DELETE FROM strava_segment_efforts WHERE rideId = :rideId")
    suspend fun deleteForRide(rideId: Long)

    /** Atomically replaces all cached efforts for [rideId] with [efforts]. */
    @Transaction
    suspend fun replaceForRide(rideId: Long, efforts: List<StravaSegmentEffortEntity>) {
        deleteForRide(rideId)
        insertAll(efforts)
    }
}
