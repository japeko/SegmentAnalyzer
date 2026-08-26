package com.segmentanalyzer.feature.history.history

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.SummaryPeriod
import com.segmentanalyzer.domain.usecase.RideSummary

data class RideHistoryUiState(
    val isLoading: Boolean = true,
    val summary: RideSummary? = null,
    val summaryPeriod: SummaryPeriod = SummaryPeriod.THIS_MONTH,
    val selectedFilter: ActivityType? = null,
    val rides: List<RideListItem> = emptyList(),
    /** Non-empty means selection mode is active — tapping a ride toggles it instead of opening it. */
    val selectedRideIds: Set<Long> = emptySet(),
    /** Non-null while the "set tag for selected rides" dialog is open. */
    val tagDialog: BulkTagDialogState? = null,
    /** Non-null while the "set activity type for selected rides" dialog is open. */
    val activityTypeDialog: BulkActivityTypeDialogState? = null,
    /** Non-null while the "delete this ride?" confirmation dialog (from a swipe) is open. */
    val pendingDeleteRide: RideListItem? = null,
    /** Non-null right after a delete is confirmed — drives the "Ride deleted — Undo" snackbar. */
    val undoDeleteRide: UndoDeleteRideState? = null,
)

/** [rideId]/[rideName] of the just-deleted ride, for the undo snackbar's message and its Compose key. */
data class UndoDeleteRideState(val rideId: Long, val rideName: String)

/** Live state of the bulk tag dialog — [tagSuggestions] narrows as [tag] is typed. */
data class BulkTagDialogState(
    val tag: String,
    val tagSuggestions: List<String> = emptyList(),
    val selectedCount: Int,
)

/** Live state of the bulk activity-type dialog — [selectedType] is null until the rider picks one. */
data class BulkActivityTypeDialogState(
    val selectedCount: Int,
    val selectedType: ActivityType? = null,
)

/** A ride pre-formatted for display — the ViewModel does the formatting, not the composable. */
data class RideListItem(
    val id: Long,
    val name: String,
    val activityType: ActivityType,
    val source: ActivitySource,
    val dateLabel: String,
    val distanceKm: Double,
    val durationLabel: String,
    val elevationGainMeters: Double,
    val avgSpeedKmh: Double,
    val isPersonalBest: Boolean,
    val elevationProfile: List<Float>,
    val tag: String?,
    /** True once the rider has opened this ride's detail screen at least once. */
    val isViewed: Boolean = false,
)
