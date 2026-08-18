package com.segmentanalyzer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One recorded GPS sample of a ride, in chronological order. */
@Entity(
    tableName = "ride_points",
    foreignKeys = [
        ForeignKey(
            entity = RideEntity::class,
            parentColumns = ["id"],
            childColumns = ["rideId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("rideId"), Index(value = ["rideId", "sequence"])],
)
data class RidePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rideId: Long,
    /** Ordinal position within the ride, for cheap ordered iteration. */
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
