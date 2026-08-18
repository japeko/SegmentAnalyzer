package com.segmentanalyzer.domain.model

/** A Strava segment the rider has starred, for analysis against local rides. */
data class Segment(
    val id: Long,
    val externalId: String,
    val name: String,
    val distanceMeters: Double,
    val averageGradePercent: Double,
    val maximumGradePercent: Double,
    val elevationGainMeters: Double,
    val climbCategory: Int,
    val city: String?,
    val state: String?,
    /** Null if Strava didn't supply coordinates for this segment — such segments are never matched to rides. */
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    /** Encoded (Google polyline format) full route, if Strava's per-segment detail fetch succeeded. */
    val polyline: String? = null,
)
