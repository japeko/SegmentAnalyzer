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
    val progressPoints: List<ProgressPoint> = emptyList(),
    val attempts: List<AttemptItem> = emptyList(),
    /** Non-null once we've confirmed via Strava that this segment isn't starred there yet. */
    val starPrompt: StarPromptState? = null,
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
    val isPersonalBest: Boolean,
    /** True if this attempt's stats/track came from a Strava segment effort, not local GPS matching. */
    val isFromStrava: Boolean,
)

data class ProgressPoint(val attemptId: Long, val normalizedY: Float, val isPersonalBest: Boolean)
