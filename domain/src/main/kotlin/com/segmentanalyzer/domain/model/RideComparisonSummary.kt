package com.segmentanalyzer.domain.model

/** Where along the segment an attempt was ahead of or behind the reference ride by the given amount. */
data class RideComparisonGapPoint(val distanceMeters: Double, val gapSeconds: Double)

data class RideComparisonAttemptSummary(
    /** e.g. "Current", "Personal Best (2026-06-01)" — however the chip identifies this ride. */
    val label: String,
    val durationSeconds: Long,
    val avgSpeedKmh: Double,
    val avgPowerWatts: Double?,
    /** Seconds ahead (negative) or behind (positive) the reference at the segment's end; null for the reference itself. */
    val finalGapSeconds: Double?,
    /**
     * The single furthest-behind point along the segment (null if never behind, or this is the
     * reference itself). Tracked separately from [bestPoint] — an early loss that gets partly
     * recovered later can be the smaller of the two swings by raw magnitude, so picking only
     * "the single biggest swing overall" can silently drop it even though it's exactly the kind
     * of moment a rider wants explained.
     */
    val worstPoint: RideComparisonGapPoint?,
    /** The single furthest-ahead point along the segment (null if never ahead, or this is the reference itself). */
    val bestPoint: RideComparisonGapPoint?,
)

/** Everything an AI insight prompt needs to explain why one ride was faster than another on this segment. */
data class RideComparisonSummary(
    val segmentName: String,
    val segmentDistanceMeters: Double,
    val referenceLabel: String,
    val attempts: List<RideComparisonAttemptSummary>,
)
