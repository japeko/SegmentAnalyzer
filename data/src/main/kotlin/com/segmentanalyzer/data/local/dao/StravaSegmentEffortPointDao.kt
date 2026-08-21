package com.segmentanalyzer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.segmentanalyzer.data.local.entity.StravaSegmentEffortPointEntity

@Dao
interface StravaSegmentEffortPointDao {

    @Query("SELECT * FROM strava_segment_effort_points WHERE effortExternalId = :effortExternalId ORDER BY sequence")
    suspend fun forEffort(effortExternalId: String): List<StravaSegmentEffortPointEntity>

    @Insert
    suspend fun insertAll(points: List<StravaSegmentEffortPointEntity>)

    @Query("DELETE FROM strava_segment_effort_points WHERE effortExternalId = :effortExternalId")
    suspend fun deleteForEffort(effortExternalId: String)

    /** Atomically replaces all cached points for [effortExternalId] with [points]. */
    @Transaction
    suspend fun replaceForEffort(effortExternalId: String, points: List<StravaSegmentEffortPointEntity>) {
        deleteForEffort(effortExternalId)
        insertAll(points)
    }
}
