package com.segmentanalyzer.domain.model

import java.time.Duration

/** One segment effort from a Strava activity — Strava's own timing/ranking for a segment. */
data class StravaSegmentEffort(
    val segmentName: String,
    val elapsedTime: Duration,
    val distanceMeters: Double,
    /** 1-10 if this effort ranks in the segment's current all-time top 10, else null. */
    val komRank: Int?,
    /** 1-3 if this is the athlete's personal top-3 time on the segment, else null. */
    val prRank: Int?,
)
