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
)
