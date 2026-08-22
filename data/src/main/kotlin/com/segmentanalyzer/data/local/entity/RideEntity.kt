package com.segmentanalyzer.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType

/**
 * A stored ride. Average/max speed are deliberately not columns here — they're derived from
 * [distanceMeters]/[durationMillis] in the domain model so there's one source of truth for pace.
 *
 * Deliberately deferred for now (documented so the next screen, Ride Analysis, only needs
 * additive migrations): per-ride max speed / avg heart rate / avg power / avg cadence as
 * nullable columns once import parses samples; a separate table for full-resolution GPS/
 * telemetry tracks — [elevationProfilePreview] here is only a lightweight ~24-point sparkline
 * for the history card thumbnail, not analysis-grade data; segment/split/route-grouping tables.
 */
@Entity(tableName = "rides", indices = [Index(value = ["externalId"], unique = true)])
data class RideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Garmin/Strava activity id, kept for de-duplication on future re-import. Null for local files. */
    val externalId: String?,
    val name: String,
    val activityType: ActivityType,
    val source: ActivitySource,
    val startTimeEpochMillis: Long,
    val durationMillis: Long,
    val distanceMeters: Double,
    val elevationGainMeters: Double,
    val isPersonalBest: Boolean,
    val elevationProfilePreview: List<Float>,
    /** Path/URI of the originally imported FIT/GPX file, if any. */
    val sourceFilePath: String?,
    val createdAtEpochMillis: Long,
    /** User-assigned free-text label, e.g. "Race" or "Training" — null if never set. */
    val tag: String? = null,
)
