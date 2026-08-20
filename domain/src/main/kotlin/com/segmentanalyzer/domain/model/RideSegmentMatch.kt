package com.segmentanalyzer.domain.model

import java.time.Duration
import java.time.Instant

/** A segment a specific ride passed through, from that ride's point of view. */
data class RideSegmentMatch(
    val attemptId: Long,
    val segmentId: Long,
    val segmentName: String,
    val segmentDistanceMeters: Double,
    val startTime: Instant,
    val duration: Duration,
    val avgSpeedKmh: Double,
    val isPersonalBest: Boolean,
)
