package com.segmentanalyzer.domain.model

/**
 * Summary stats derived from a specific Strava segment effort's point-by-point streams
 * (`GET /segment_efforts/{id}/streams`). Sensor fields are null when the ride had no reading
 * for them (e.g. no power meter), not zero.
 */
data class StravaSegmentEffortDetail(
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val elevationGainMeters: Double,
    val avgWatts: Double?,
    val avgHeartRateBpm: Double?,
    val avgCadenceRpm: Double?,
)
