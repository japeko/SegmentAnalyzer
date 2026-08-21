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
 *
 * A row can also be a pseudo-attempt derived from a Strava segment effort rather than local GPS
 * matching — marked by a non-null [stravaEffortExternalId], with [entryPointSequence]/
 * [exitPointSequence] left null since its track lives in `strava_segment_effort_points`, not
 * [RidePointEntity], keyed by [stravaEffortExternalId] instead (see the separate unique index).
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
        Index(value = ["stravaEffortExternalId"], unique = true),
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
    /** [RidePointEntity.sequence] at segment entry/exit, so the sub-track can be re-sliced on demand. Null for a Strava-derived row. */
    val entryPointSequence: Int?,
    val exitPointSequence: Int?,
    val createdAtEpochMillis: Long,
    /** Non-null marks this row as derived from a Strava segment effort — see the class doc. */
    val stravaEffortExternalId: String? = null,
)
