package com.segmentanalyzer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A cached Strava segment effort for a ride. Rows for a ride are fully replaced on each fetch
 * (delete-then-insert), so this always reflects the most recently fetched data, not a history.
 */
@Entity(
    tableName = "strava_segment_efforts",
    foreignKeys = [
        ForeignKey(
            entity = RideEntity::class,
            parentColumns = ["id"],
            childColumns = ["rideId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("rideId")],
)
data class StravaSegmentEffortEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rideId: Long,
    /** Strava's id for this specific effort — used to fetch its point-by-point streams. */
    val effortExternalId: String,
    /** Strava's id for the segment this effort was on — not this effort itself. */
    val segmentExternalId: String,
    val segmentName: String,
    val elapsedTimeSeconds: Long,
    val distanceMeters: Double,
    val komRank: Int?,
    val prRank: Int?,
    /**
     * Pace/power/HR/cadence detail for this effort, fetched lazily on demand (not with the rest
     * of the row) — null until the user expands it, so `avgSpeedKmh == null` is what marks "not
     * fetched yet" versus "fetched." Kept alongside the row (not delete/replaced with the ride's
     * effort list) precisely so it survives for comparing this effort against another ride's.
     */
    val avgSpeedKmh: Double? = null,
    val maxSpeedKmh: Double? = null,
    val elevationGainMeters: Double? = null,
    val avgWatts: Double? = null,
    val avgHeartRateBpm: Double? = null,
    val avgCadenceRpm: Double? = null,
)
