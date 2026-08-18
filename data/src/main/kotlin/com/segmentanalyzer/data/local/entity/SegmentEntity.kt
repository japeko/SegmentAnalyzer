package com.segmentanalyzer.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A starred Strava segment, synced locally for analysis against rides. */
@Entity(tableName = "segments", indices = [Index(value = ["externalId"], unique = true)])
data class SegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The Strava segment id, kept for de-duplication on re-sync. */
    val externalId: String,
    val name: String,
    val distanceMeters: Double,
    val averageGradePercent: Double,
    val maximumGradePercent: Double,
    val elevationGainMeters: Double,
    val climbCategory: Int,
    val city: String?,
    val state: String?,
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val polyline: String? = null,
)
