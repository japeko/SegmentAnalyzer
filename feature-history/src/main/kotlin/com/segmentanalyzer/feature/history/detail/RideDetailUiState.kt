package com.segmentanalyzer.feature.history.detail

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType

data class RideDetailUiState(
    val isLoading: Boolean = true,
    val ride: RideDetailInfo? = null,
    val stravaSegmentEfforts: StravaEffortsUiState = StravaEffortsUiState.Idle,
    /** The effort (if any) whose full pace/power/HR detail is currently expanded below its row. */
    val expandedSegmentEffortDetail: ExpandedSegmentEffortDetail? = null,
    /** Non-null while the rename/tag dialog is open. */
    val editDialog: EditRideDialogState? = null,
)

/** Live state of the rename/tag/type dialog — [tagSuggestions] narrows as [tag] is typed. */
data class EditRideDialogState(
    val name: String,
    val tag: String,
    val activityType: ActivityType,
    val tagSuggestions: List<String> = emptyList(),
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
    val effortExternalId: String,
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
 * [effortIndex] identifies which row is expanded, so tapping one effort doesn't also expand
 * another row that happens to be on the same segment (a ride can lap a segment more than once).
 */
data class ExpandedSegmentEffortDetail(
    val effortIndex: Int,
    val effortExternalId: String,
    val state: StravaEffortDetailUiState,
)

/** State of an on-demand fetch of one effort's pace/power/HR/cadence detail, for comparison. */
sealed interface StravaEffortDetailUiState {
    data object Loading : StravaEffortDetailUiState
    data class Loaded(val detail: StravaSegmentEffortDetailItem) : StravaEffortDetailUiState
    data class Error(val message: String) : StravaEffortDetailUiState
}

/** Pace/power/HR/cadence summary for one effort, pre-formatted for display. */
data class StravaSegmentEffortDetailItem(
    val avgSpeedLabel: String,
    val maxSpeedLabel: String,
    val elevationGainLabel: String,
    val avgWattsLabel: String?,
    val avgHeartRateLabel: String?,
    val avgCadenceLabel: String?,
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
    val tag: String?,
)
