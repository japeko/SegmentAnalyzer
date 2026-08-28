package com.segmentanalyzer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One recorded GPS sample of a [GuestAttemptEntity]'s entry..exit sub-track — only the matched
 * segment slice is kept, not the friend's whole ride, distance re-based so the first point is 0
 * (same convention [com.segmentanalyzer.data.repository.SegmentAttemptRepositoryImpl] uses when
 * slicing a local ride's own track).
 */
@Entity(
    tableName = "guest_attempt_points",
    foreignKeys = [
        ForeignKey(
            entity = GuestAttemptEntity::class,
            parentColumns = ["id"],
            childColumns = ["guestAttemptId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("guestAttemptId"), Index(value = ["guestAttemptId", "sequence"])],
)
data class GuestAttemptPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val guestAttemptId: Long,
    val sequence: Int,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Float?,
    val timestampEpochMillis: Long,
    val cumulativeDistanceMeters: Double,
    val heartRateBpm: Int?,
    val cadenceRpm: Int?,
    val powerWatts: Int?,
)
