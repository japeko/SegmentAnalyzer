package com.segmentanalyzer.feature.history.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.common.format.toRideCardDate
import com.segmentanalyzer.common.format.toRideClock
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.SummaryPeriod
import com.segmentanalyzer.domain.usecase.DeleteRideUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideHistoryUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideSummaryUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideTagsUseCase
import com.segmentanalyzer.domain.usecase.ObserveViewedRideIdsUseCase
import com.segmentanalyzer.domain.usecase.RestoreRideUseCase
import com.segmentanalyzer.domain.usecase.RideSummary
import com.segmentanalyzer.domain.usecase.SetActivityTypeForRidesUseCase
import com.segmentanalyzer.domain.usecase.SetTagForRidesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RideHistoryViewModel @Inject constructor(
    observeRideHistory: ObserveRideHistoryUseCase,
    observeRideSummary: ObserveRideSummaryUseCase,
    observeRideTags: ObserveRideTagsUseCase,
    observeViewedRideIds: ObserveViewedRideIdsUseCase,
    private val setTagForRides: SetTagForRidesUseCase,
    private val setActivityTypeForRides: SetActivityTypeForRidesUseCase,
    private val deleteRide: DeleteRideUseCase,
    private val restoreRide: RestoreRideUseCase,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<ActivityType?>(null)
    private val selectedPeriod = MutableStateFlow(SummaryPeriod.THIS_MONTH)

    /** Non-empty means selection mode is active. */
    private val selectedRideIds = MutableStateFlow<Set<Long>>(emptySet())

    /** Non-null while the bulk tag dialog is open, holding its in-progress (unsaved) tag text. */
    private val tagDialogText = MutableStateFlow<String?>(null)

    /** Whether the bulk activity-type dialog is open, and the type picked so far (unsaved). */
    private val activityTypeDialogOpen = MutableStateFlow(false)
    private val activityTypeDialogSelection = MutableStateFlow<ActivityType?>(null)

    /** Non-null while the "delete this ride?" confirmation dialog (from a swipe) is open. */
    private val pendingDeleteRideId = MutableStateFlow<Long?>(null)

    /** The just-deleted ride, kept around so the "Undo" snackbar action can restore it. */
    private val pendingUndoRide = MutableStateFlow<Ride?>(null)

    /** The most recent ride list, so a delete/undo can look up a full [Ride] by id without a second subscription. */
    private var latestRides: List<Ride> = emptyList()

    private val coreState = combine(
        combine(selectedFilter, selectedPeriod) { filter, period -> filter to period }
            .flatMapLatest { (filter, period) -> observeRideHistory(filter, period) },
        selectedPeriod.flatMapLatest { observeRideSummary(it) },
        selectedFilter,
        selectedPeriod,
    ) { rides, summary, filter, period ->
        latestRides = rides
        CoreHistory(rides, summary, filter, period)
    }

    private val dialogsState = combine(
        tagDialogText,
        activityTypeDialogOpen,
        activityTypeDialogSelection,
        pendingDeleteRideId,
        pendingUndoRide,
    ) { tagText, typeDialogOpen, typeSelection, deleteRideId, undoRide ->
        DialogsState(tagText, typeDialogOpen, typeSelection, deleteRideId, undoRide)
    }

    val uiState: StateFlow<RideHistoryUiState> = combine(
        coreState,
        selectedRideIds,
        dialogsState,
        observeRideTags(),
        observeViewedRideIds(),
    ) { core, selectedIds, dialogs, tags, viewedIds ->
        RideHistoryUiState(
            isLoading = false,
            summary = core.summary,
            summaryPeriod = core.period,
            selectedFilter = core.filter,
            rides = core.rides.map { it.toListItem(isViewed = it.id in viewedIds) },
            selectedRideIds = selectedIds,
            tagDialog = dialogs.tagText?.let { text ->
                BulkTagDialogState(
                    tag = text,
                    tagSuggestions = tags.filter {
                        text.isNotBlank() && it.contains(text, ignoreCase = true) && !it.equals(text, ignoreCase = true)
                    },
                    selectedCount = selectedIds.size,
                )
            },
            activityTypeDialog = if (dialogs.activityTypeDialogOpen) {
                BulkActivityTypeDialogState(selectedCount = selectedIds.size, selectedType = dialogs.activityTypeSelection)
            } else {
                null
            },
            pendingDeleteRide = dialogs.deleteRideId?.let { id -> core.rides.find { it.id == id } }
                ?.toListItem(isViewed = false),
            undoDeleteRide = dialogs.undoRide?.let { UndoDeleteRideState(rideId = it.id, rideName = it.name) },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RideHistoryUiState(),
    )

    fun onFilterSelected(type: ActivityType?) {
        selectedFilter.value = type
    }

    fun onPeriodSelected(period: SummaryPeriod) {
        selectedPeriod.value = period
    }

    fun onRideLongPress(rideId: Long) {
        selectedRideIds.value = selectedRideIds.value + rideId
    }

    fun onRideSelectionToggled(rideId: Long) {
        val current = selectedRideIds.value
        selectedRideIds.value = if (rideId in current) current - rideId else current + rideId
    }

    fun onExitSelectionMode() {
        selectedRideIds.value = emptySet()
    }

    fun onSetTagClick() {
        if (selectedRideIds.value.isEmpty()) return
        tagDialogText.value = ""
    }

    fun onTagDialogValueChange(text: String) {
        tagDialogText.value = text
    }

    fun onTagSuggestionClick(tag: String) {
        tagDialogText.value = tag
    }

    fun onDismissTagDialog() {
        tagDialogText.value = null
    }

    fun onConfirmSetTag() {
        val tag = tagDialogText.value ?: return
        val ids = selectedRideIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            setTagForRides(ids, tag)
            tagDialogText.value = null
            selectedRideIds.value = emptySet()
        }
    }

    fun onSetActivityTypeClick() {
        if (selectedRideIds.value.isEmpty()) return
        activityTypeDialogSelection.value = null
        activityTypeDialogOpen.value = true
    }

    fun onActivityTypeDialogSelected(type: ActivityType) {
        activityTypeDialogSelection.value = type
    }

    fun onDismissActivityTypeDialog() {
        activityTypeDialogOpen.value = false
        activityTypeDialogSelection.value = null
    }

    fun onConfirmSetActivityType() {
        val type = activityTypeDialogSelection.value ?: return
        val ids = selectedRideIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            setActivityTypeForRides(ids, type)
            activityTypeDialogOpen.value = false
            activityTypeDialogSelection.value = null
            selectedRideIds.value = emptySet()
        }
    }

    /** Swiped a ride left — asks for confirmation before actually deleting anything. */
    fun onDeleteRideRequested(rideId: Long) {
        pendingDeleteRideId.value = rideId
    }

    fun onDismissDeleteRide() {
        pendingDeleteRideId.value = null
    }

    fun onConfirmDeleteRide() {
        val rideId = pendingDeleteRideId.value ?: return
        val ride = latestRides.find { it.id == rideId } ?: return
        viewModelScope.launch {
            deleteRide(rideId)
            pendingDeleteRideId.value = null
            pendingUndoRide.value = ride
        }
    }

    /** The "Undo" snackbar action was tapped — restores the ride that was just deleted. */
    fun onUndoDeleteRideClick() {
        val ride = pendingUndoRide.value ?: return
        viewModelScope.launch { restoreRide(ride) }
        pendingUndoRide.value = null
    }

    /** The undo snackbar timed out or was swiped away without tapping "Undo" — the delete stands. */
    fun onUndoDeleteRideSnackbarDismissed() {
        pendingUndoRide.value = null
    }
}

private data class CoreHistory(
    val rides: List<Ride>,
    val summary: RideSummary,
    val filter: ActivityType?,
    val period: SummaryPeriod,
)

private data class DialogsState(
    val tagText: String?,
    val activityTypeDialogOpen: Boolean,
    val activityTypeSelection: ActivityType?,
    val deleteRideId: Long?,
    val undoRide: Ride?,
)

private fun Ride.toListItem(isViewed: Boolean): RideListItem = RideListItem(
    id = id,
    name = name,
    activityType = activityType,
    source = source,
    dateLabel = startTime.toRideCardDate(),
    distanceKm = distanceMeters / 1000.0,
    durationLabel = duration.toRideClock(),
    elevationGainMeters = elevationGainMeters,
    avgSpeedKmh = averageSpeedKmh,
    isPersonalBest = isPersonalBest,
    elevationProfile = elevationProfile,
    tag = tag,
    isViewed = isViewed,
)
