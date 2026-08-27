package com.segmentanalyzer.domain.model

/**
 * One recorded GPS sample of a Strava segment effort, in chronological order — from the same
 * point-by-point streams as [StravaSegmentEffortDetail]. Meant for the Segments page to plot a
 * specific effort's route/pace, e.g. comparing the same segment across two different rides.
 */
data class StravaSegmentEffortPoint(
    /** Seconds elapsed since the start of this effort. */
    val timeSeconds: Int,
    val distanceMeters: Double,
    val latitude: Double,
    val longitude: Double,
    /** Null if Strava's `altitude` stream was missing for this effort (rare, but not guaranteed). */
    val elevationMeters: Double? = null,
)
