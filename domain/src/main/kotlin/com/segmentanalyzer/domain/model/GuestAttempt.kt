package com.segmentanalyzer.domain.model

import java.time.Duration
import java.time.Instant

/**
 * A segment attempt imported from someone else's FIT file — kept entirely separate from
 * [SegmentAttempt]. Never counted toward your own Personal Best, the Records page, or ride
 * history; only ever surfaced as a labeled, addable line in Compare Rides and as a distinctly
 * tagged entry on Segment Detail's attempt list.
 */
data class GuestAttempt(
    val id: Long,
    val segmentId: Long,
    /** Whatever the importer typed in — the FIT format has no rider-name field reliably present in a real exported activity file. */
    val riderName: String,
    val importedAt: Instant,
    val startTime: Instant,
    val duration: Duration,
    val avgSpeedKmh: Double,
    val elevationGainMeters: Double,
)
