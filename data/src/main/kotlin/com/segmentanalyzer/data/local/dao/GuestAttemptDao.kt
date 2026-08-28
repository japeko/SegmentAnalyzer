package com.segmentanalyzer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.segmentanalyzer.data.local.entity.GuestAttemptEntity
import com.segmentanalyzer.data.local.entity.GuestAttemptPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GuestAttemptDao {

    @Insert
    suspend fun insert(attempt: GuestAttemptEntity): Long

    @Insert
    suspend fun insertPoints(points: List<GuestAttemptPointEntity>)

    @Query("SELECT * FROM guest_attempts WHERE segmentId = :segmentId ORDER BY importedAtEpochMillis DESC")
    fun observeForSegment(segmentId: Long): Flow<List<GuestAttemptEntity>>

    @Query("SELECT * FROM guest_attempt_points WHERE guestAttemptId = :guestAttemptId ORDER BY sequence ASC")
    suspend fun pointsForAttempt(guestAttemptId: Long): List<GuestAttemptPointEntity>

    @Query("DELETE FROM guest_attempts WHERE id = :id")
    suspend fun delete(id: Long)
}
