package com.segmentanalyzer.domain.model

import java.time.Instant

/** One recorded GPS sample of a ride, in chronological order. */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Float?,
    val timestamp: Instant,
    val cumulativeDistanceMeters: Double,
    val heartRateBpm: Int? = null,
    val cadenceRpm: Int? = null,
    val powerWatts: Int? = null,
)
