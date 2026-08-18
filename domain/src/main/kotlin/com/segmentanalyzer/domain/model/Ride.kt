package com.segmentanalyzer.domain.model

import java.time.Duration
import java.time.Instant

/**
 * A single imported ride and the summary stats derived from it.
 *
 * Average and max speed are intentionally not stored fields — they're computed from
 * [distanceMeters] and [duration] wherever needed, so there is exactly one source of truth
 * for a ride's pace.
 */
data class Ride(
    val id: Long,
    val name: String,
    val activityType: ActivityType,
    val source: ActivitySource,
    val startTime: Instant,
    val duration: Duration,
    val distanceMeters: Double,
    val elevationGainMeters: Double,
    val isPersonalBest: Boolean,
    val elevationProfile: List<Float>,
    val sourceFilePath: String?,
    /** Garmin/Strava activity id, used to avoid re-importing the same ride. Null for local files. */
    val externalId: String? = null,
) {
    val averageSpeedKmh: Double
        get() {
            val hours = duration.seconds / 3600.0
            return if (hours <= 0.0) 0.0 else (distanceMeters / 1000.0) / hours
        }
}
