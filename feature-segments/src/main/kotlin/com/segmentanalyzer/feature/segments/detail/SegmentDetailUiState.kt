package com.segmentanalyzer.feature.segments.detail

import com.segmentanalyzer.domain.model.LatLng
import com.segmentanalyzer.domain.model.Segment

data class SegmentDetailUiState(
    val isLoading: Boolean = true,
    val segment: Segment? = null,
    /** The segment's route, decoded from Strava's polyline if available, else just [start, end]. */
    val routePoints: List<LatLng> = emptyList(),
    val personalBest: AttemptItem? = null,
    val personalBestDeltaSeconds: Long? = null,
    /** Excludes [excludedAttempts] — nothing the rider has hidden shows a dot. */
    val progressPoints: List<ProgressPoint> = emptyList(),
    val attempts: List<AttemptItem> = emptyList(),
    /** Attempts the rider has swiped out of [attempts] — hidden from the chart, shown in a separate section they can restore from. */
    val excludedAttempts: List<AttemptItem> = emptyList(),
    /** Non-null once we've confirmed via Strava that this segment isn't starred there yet. */
    val starPrompt: StarPromptState? = null,
    /** The attempt last tapped in "All Attempts" — stays highlighted on the chart across nav to Compare Rides and back. */
    val selectedAttemptId: Long? = null,
    /** True when [attempts]/[excludedAttempts] are shown newest-first instead of the default oldest-first. */
    val attemptsReversed: Boolean = false,
    /** True while Strava isn't connected — this segment's star-status check was skipped rather than doomed to fail. */
    val stravaNotConnected: Boolean = false,
)

/** Live state of the "star this segment?" prompt. */
data class StarPromptState(val isSaving: Boolean = false)

data class AttemptItem(
    val id: Long,
    val rideId: Long,
    val rideName: String,
    val dateLabel: String,
    /** "Ride 1", "Ride 2", ... — this attempt's lap number among all attempts from the same ride. */
    val lapLabel: String,
    val durationLabel: String,
    val deltaVsPrSeconds: Long,
    /** 1/2/3 for the segment's three fastest (non-excluded) attempts, else null. 1 is the personal best. */
    val rank: Int?,
    /** True if this attempt's stats/track came from a Strava segment effort, not local GPS matching. */
    val isFromStrava: Boolean,
)

/** [rank] is 1/2/3 for the segment's three fastest (non-excluded) attempts, else null — same as [AttemptItem.rank]. */
data class ProgressPoint(val attemptId: Long, val normalizedY: Float, val rank: Int?)
