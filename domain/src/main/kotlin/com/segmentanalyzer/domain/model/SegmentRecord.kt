package com.segmentanalyzer.domain.model

import java.time.Duration
import java.time.Instant

/** A segment's current fastest attempt — i.e. the personal record for that segment. */
data class SegmentRecord(
    val attemptId: Long,
    val segmentId: Long,
    val segmentName: String,
    val segmentDistanceMeters: Double,
    val rideId: Long,
    val rideName: String,
    val rideSource: ActivitySource,
    val startTime: Instant,
    val duration: Duration,
    val avgSpeedKmh: Double,
)
