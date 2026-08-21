package com.segmentanalyzer.domain.model

import java.time.Duration
import java.time.Instant

/** One of the athlete's own past efforts on a segment, from Strava's effort history for it. */
data class StravaSegmentEffortHistoryEntry(
    val startTime: Instant,
    val elapsedTime: Duration,
    val distanceMeters: Double,
    /** 1-10 if this effort ranks in the segment's current all-time top 10, else null. */
    val komRank: Int?,
    /** 1-3 if this is the athlete's personal top-3 time on the segment, else null. */
    val prRank: Int?,
)
