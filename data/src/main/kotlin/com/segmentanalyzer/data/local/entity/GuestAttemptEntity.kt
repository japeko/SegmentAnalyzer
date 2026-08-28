package com.segmentanalyzer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A segment attempt imported from someone else's FIT file — deliberately not a
 * [SegmentAttemptEntity]/[RideEntity]. Never read by Personal Best, Records, or ride-history
 * queries; only ever surfaced explicitly via [com.segmentanalyzer.data.local.dao.GuestAttemptDao].
 */
@Entity(
    tableName = "guest_attempts",
    foreignKeys = [
        ForeignKey(
            entity = SegmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["segmentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("segmentId")],
)
data class GuestAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val segmentId: Long,
    /** Whatever the importer typed in — the FIT format has no rider-name field reliably present in a real exported activity file. */
    val riderName: String,
    val importedAtEpochMillis: Long,
    val startTimeEpochMillis: Long,
    val durationMillis: Long,
    val avgSpeedKmh: Double,
    val elevationGainMeters: Double,
)
