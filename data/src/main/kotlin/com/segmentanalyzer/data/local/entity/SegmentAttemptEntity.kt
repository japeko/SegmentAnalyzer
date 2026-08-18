package com.segmentanalyzer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A ride matched against a segment: the ride's track passed within [SEGMENT_PROXIMITY_METERS]
 * (see the matcher) of both the segment's start and end. One row per (segment, ride) pair — a
 * ride that laps the same segment twice within itself only produces one attempt (first pass).
 */
@Entity(
    tableName = "segment_attempts",
    foreignKeys = [
        ForeignKey(
            entity = SegmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["segmentId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RideEntity::class,
            parentColumns = ["id"],
            childColumns = ["rideId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("segmentId"),
        Index("rideId"),
        Index(value = ["segmentId", "rideId"], unique = true),
    ],
)
data class SegmentAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val segmentId: Long,
    val rideId: Long,
    val startTimeEpochMillis: Long,
    val durationMillis: Long,
    val avgSpeedKmh: Double,
    val elevationGainMeters: Double,
    val avgPowerWatts: Double?,
    /** [RidePointEntity.sequence] at segment entry/exit, so the sub-track can be re-sliced on demand. */
    val entryPointSequence: Int,
    val exitPointSequence: Int,
    val createdAtEpochMillis: Long,
)
