package com.segmentanalyzer.domain.model

import java.time.Duration
import java.time.Instant

/** A single ride's pass through a segment, matched by GPS proximity to the segment's start/end. */
data class SegmentAttempt(
    val id: Long,
    val segmentId: Long,
    val rideId: Long,
    val rideName: String,
    val rideSource: ActivitySource,
    val startTime: Instant,
    val duration: Duration,
    val avgSpeedKmh: Double,
    val elevationGainMeters: Double,
    val avgPowerWatts: Double?,
    /** True if this attempt's stats/track came from a Strava segment effort, not local GPS matching. */
    val isFromStrava: Boolean = false,
)
