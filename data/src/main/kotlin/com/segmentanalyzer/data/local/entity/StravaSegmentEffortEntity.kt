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
    /** Strava's id for the segment this effort was on — not this effort itself. */
    val segmentExternalId: String,
    val segmentName: String,
    val elapsedTimeSeconds: Long,
    val distanceMeters: Double,
    val komRank: Int?,
    val prRank: Int?,
)
