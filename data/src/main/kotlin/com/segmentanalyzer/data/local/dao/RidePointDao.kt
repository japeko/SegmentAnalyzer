package com.segmentanalyzer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.segmentanalyzer.data.local.entity.RidePointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RidePointDao {

    @Insert
    suspend fun insertAll(points: List<RidePointEntity>)

    @Query("SELECT * FROM ride_points WHERE rideId = :rideId ORDER BY sequence ASC")
    suspend fun pointsForRide(rideId: Long): List<RidePointEntity>

    /** Every ride id that has at least one stored track point. */
    @Query("SELECT DISTINCT rideId FROM ride_points")
    suspend fun rideIdsWithTracks(): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM ride_points WHERE rideId = :rideId)")
    fun observeHasTrack(rideId: Long): Flow<Boolean>
}
