package com.segmentanalyzer.feature.history.detail

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType

data class RideDetailUiState(
    val isLoading: Boolean = true,
    val ride: RideDetailInfo? = null,
    /** Whether this ride has a stored GPS track — only FIT/GPX imports do. */
    val hasTrack: Boolean = false,
    val matchedSegments: List<MatchedSegmentItem> = emptyList(),
    val stravaSegmentEfforts: StravaEffortsUiState = StravaEffortsUiState.Idle,
    /** The segment (if any) whose full Strava effort history is currently expanded below its row. */
    val expandedSegmentEffortHistory: ExpandedSegmentEffortHistory? = null,
)

/** State of an on-demand fetch of this ride's Strava segment effort data. */
sealed interface StravaEffortsUiState {
    data object Idle : StravaEffortsUiState
    data object Loading : StravaEffortsUiState
    data class Loaded(val efforts: List<StravaSegmentEffortItem>) : StravaEffortsUiState
    data class Error(val message: String) : StravaEffortsUiState
}

/** One Strava segment effort, pre-formatted for display. */
data class StravaSegmentEffortItem(
    val segmentExternalId: String,
    val segmentName: String,
    val elapsedTimeLabel: String,
    val distanceKm: Double,
    /** 1-10 if this effort is in the segment's current all-time top 10, else null. */
    val komRank: Int?,
    /** 1-3 if this is the athlete's personal top-3 time on the segment, else null. */
    val prRank: Int?,
)

/**
 * [effortIndex] identifies which row is expanded — not [segmentExternalId] alone, since a ride
 * can pass through the same segment more than once (e.g. lapping a descent), producing multiple
 * rows that would otherwise all expand together.
 */
data class ExpandedSegmentEffortHistory(
    val effortIndex: Int,
    val segmentExternalId: String,
    val state: StravaEffortHistoryUiState,
)

/** State of an on-demand fetch of a segment's full Strava effort history, for comparison. */
sealed interface StravaEffortHistoryUiState {
    data object Loading : StravaEffortHistoryUiState
    data class Loaded(val entries: List<StravaSegmentEffortHistoryItem>) : StravaEffortHistoryUiState
    data class Error(val message: String) : StravaEffortHistoryUiState
}

/** One past effort on a segment, pre-formatted for display. */
data class StravaSegmentEffortHistoryItem(
    val dateLabel: String,
    val elapsedTimeLabel: String,
    val distanceKm: Double,
    val komRank: Int?,
    val prRank: Int?,
)

/** A ride pre-formatted for display — the ViewModel does the formatting, not the composable. */
data class RideDetailInfo(
    val name: String,
    val activityType: ActivityType,
    val source: ActivitySource,
    val dateLabel: String,
    val distanceKm: Double,
    val durationLabel: String,
    val elevationGainMeters: Double,
    val avgSpeedKmh: Double,
    val elevationProfile: List<Float>,
    val isPersonalBest: Boolean,
)

/** A starred Strava segment this ride passed through. */
data class MatchedSegmentItem(
    val attemptId: Long,
    val segmentId: Long,
    val name: String,
    val distanceKm: Double,
    val dateLabel: String,
    val durationLabel: String,
    val avgSpeedKmh: Double,
    val isPersonalBest: Boolean,
)
