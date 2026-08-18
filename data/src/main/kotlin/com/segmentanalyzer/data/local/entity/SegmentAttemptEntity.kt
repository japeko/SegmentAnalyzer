package com.segmentanalyzer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A ride matched against a segment: the ride's track passed within [SEGMENT_PROXIMITY_METERS]
 * (see the matcher) of both the segment's start and end. A ride that laps the same segment
 * several times within itself produces one row per lap — uniqueness is on
 * (segmentId, rideId, entryPointSequence) so distinct laps of the same ride/segment pair can
 * coexist, while re-running the matcher on already-matched data stays idempotent.
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
        Index(value = ["segmentId", "rideId", "entryPointSequence"], unique = true),
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
