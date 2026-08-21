package com.segmentanalyzer.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One recorded GPS sample of a Strava segment effort, in chronological order. Not linked with a
 * Room [androidx.room.ForeignKey] to `strava_segment_efforts` since `effortExternalId` isn't a
 * unique/indexed column there — rows are instead fully replaced per effort on each fetch.
 */
@Entity(
    tableName = "strava_segment_effort_points",
    indices = [Index(value = ["effortExternalId", "sequence"])],
)
data class StravaSegmentEffortPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Strava's id for the effort this point belongs to — see [StravaSegmentEffortEntity.effortExternalId]. */
    val effortExternalId: String,
    /** Ordinal position within the effort, for cheap ordered iteration. */
    val sequence: Int,
    val timeSeconds: Int,
    val distanceMeters: Double,
    val latitude: Double,
    val longitude: Double,
)
