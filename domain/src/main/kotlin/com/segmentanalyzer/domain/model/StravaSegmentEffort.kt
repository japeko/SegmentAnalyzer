package com.segmentanalyzer.domain.model

import java.time.Duration

/** One segment effort from a Strava activity — Strava's own timing/ranking for a segment. */
data class StravaSegmentEffort(
    /** Strava's id for this specific effort — used to fetch its point-by-point streams. */
    val effortExternalId: String,
    /** Strava's id for the segment this effort was on — not this effort itself. */
    val segmentExternalId: String,
    val segmentName: String,
    val elapsedTime: Duration,
    val distanceMeters: Double,
    /** 1-10 if this effort ranks in the segment's current all-time top 10, else null. */
    val komRank: Int?,
    /** 1-3 if this is the athlete's personal top-3 time on the segment, else null. */
    val prRank: Int?,
)
