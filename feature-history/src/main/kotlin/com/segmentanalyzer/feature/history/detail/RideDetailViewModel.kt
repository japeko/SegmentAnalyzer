package com.segmentanalyzer.feature.history.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.common.format.toRideCardDate
import com.segmentanalyzer.common.format.toRideClock
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.StravaConnectionState
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.model.StravaSegmentEffortDetail
import com.segmentanalyzer.domain.usecase.FetchStravaSegmentEffortDetailUseCase
import com.segmentanalyzer.domain.usecase.FetchStravaSegmentEffortsUseCase
import com.segmentanalyzer.domain.usecase.MarkRideViewedUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideTagsUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideUseCase
import com.segmentanalyzer.domain.usecase.ObserveStravaConnectionStateUseCase
import com.segmentanalyzer.domain.usecase.ObserveStravaSegmentEffortsUseCase
import com.segmentanalyzer.domain.usecase.SaveStravaSegmentEffortAttemptUseCase
import com.segmentanalyzer.domain.usecase.UpdateRideUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RideDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeRide: ObserveRideUseCase,
    observeStravaSegmentEfforts: ObserveStravaSegmentEffortsUseCase,
    observeStravaConnectionState: ObserveStravaConnectionStateUseCase,
    observeRideTags: ObserveRideTagsUseCase,
    private val fetchStravaSegmentEfforts: FetchStravaSegmentEffortsUseCase,
    private val fetchStravaSegmentEffortDetail: FetchStravaSegmentEffortDetailUseCase,
    private val saveStravaSegmentEffortAttempt: SaveStravaSegmentEffortAttemptUseCase,
    private val updateRide: UpdateRideUseCase,
    markRideViewed: MarkRideViewedUseCase,
) : ViewModel() {

    private val rideId: Long = checkNotNull(savedStateHandle["rideId"])

    init {
        viewModelScope.launch { markRideViewed(rideId) }
        // No local segment-matching cache on this branch — go straight to Strava's live API as
        // soon as the ride is known, instead of waiting for a manual "fetch" tap. Skips the call
        // entirely (and reactively retries) while Strava isn't connected, rather than firing a
        // network call doomed to fail with an auth error every time.
        viewModelScope.launch {
            val ride = observeRide(rideId).filterNotNull().first()
            observeStravaConnectionState().collect { state ->
                if (state is StravaConnectionState.Connected) fetchSegments(ride)
            }
        }
    }

    /** The ride from the most recent [uiState] emission, so onFetchStravaSegmentsClick can read it without a lag-prone second subscription. */
    private var latestRide: Ride? = null

    /** The Strava efforts from the most recent cache emission, so a click can check for already-persisted detail first. */
    private var latestEfforts: List<StravaSegmentEffort> = emptyList()

    /**
     * Overrides the cache-derived Strava state once a fetch has been triggered in this
     * ViewModel instance (Loading/Loaded/Error). Null means "show whatever's cached," so a ride
     * with previously-fetched data opens already populated instead of forcing another fetch.
     */
    private val stravaEffortsOverride = MutableStateFlow<StravaEffortsUiState?>(null)

    /** Which effort's full pace/power/HR detail is expanded, and its fetch state. Null = none expanded. */
    private val expandedSegmentEffortDetail = MutableStateFlow<ExpandedSegmentEffortDetail?>(null)

    /** Non-null while the rename/tag dialog is open, holding its in-progress (unsaved) field values. */
    private val editRequest = MutableStateFlow<EditRideRequest?>(null)

    /** Non-empty means selection mode is active on "Segments in this Ride" — keyed by effortExternalId. */
    private val selectedEffortIds = MutableStateFlow<Set<String>>(emptySet())

    /** True while a bulk "Get Strava Data" fetch for [selectedEffortIds] is in flight. */
    private val isFetchingSelectedEfforts = MutableStateFlow(false)

    private val coreState = combine(
        observeRide(rideId),
        observeStravaSegmentEfforts(rideId),
        stravaEffortsOverride,
        observeStravaConnectionState(),
    ) { ride, cachedEfforts, override, connectionState ->
        latestRide = ride
        latestEfforts = cachedEfforts
        CoreRideDetail(
            ride = ride?.toInfo(),
            stravaSegmentEfforts = when {
                connectionState !is StravaConnectionState.Connected -> StravaEffortsUiState.NotConnected
                else -> override ?: cachedEfforts.toUiState()
            },
        )
    }

    val uiState = combine(
        coreState,
        expandedSegmentEffortDetail,
        observeRideTags(),
        editRequest,
        combine(selectedEffortIds, isFetchingSelectedEfforts) { selectedIds, fetching -> selectedIds to fetching },
    ) { core, expandedDetail, tags, edit, (selectedIds, fetching) ->
        RideDetailUiState(
            isLoading = false,
            ride = core.ride,
            stravaSegmentEfforts = core.stravaSegmentEfforts,
            expandedSegmentEffortDetail = expandedDetail,
            editDialog = edit?.let { request ->
                EditRideDialogState(
                    name = request.name,
                    tag = request.tag,
                    activityType = request.activityType,
                    tagSuggestions = tags.filter {
                        request.tag.isNotBlank() && it.contains(request.tag, ignoreCase = true) && !it.equals(request.tag, ignoreCase = true)
                    },
                )
            },
            selectedEffortIds = selectedIds,
            isFetchingSelectedEfforts = fetching,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RideDetailUiState(),
    )

    fun onFetchStravaSegmentsClick() {
        val ride = latestRide ?: return
        viewModelScope.launch { fetchSegments(ride) }
    }

    private suspend fun fetchSegments(ride: Ride) {
        stravaEffortsOverride.value = StravaEffortsUiState.Loading
        stravaEffortsOverride.value = fetchStravaSegmentEfforts(ride).fold(
            onSuccess = { efforts -> StravaEffortsUiState.Loaded(efforts.map { it.toItem() }) },
            onFailure = { throwable ->
                StravaEffortsUiState.Error(throwable.message ?: "Couldn't fetch Strava segment data.")
            },
        )
    }

    /**
     * Toggles the pace/power/HR detail panel for one segment-effort row (identified by
     * [effortIndex], since a ride can pass through the same segment more than once): shows
     * already-cached detail immediately, fetches (and persists) it if not cached yet, or
     * collapses the panel if it's already open.
     */
    fun onStravaSegmentEffortClick(effortIndex: Int, effortExternalId: String) {
        if (expandedSegmentEffortDetail.value?.effortIndex == effortIndex) {
            expandedSegmentEffortDetail.value = null
            return
        }
        val effort = latestEfforts.find { it.effortExternalId == effortExternalId }
        val cachedDetail = effort?.detail
        if (effort != null && cachedDetail != null) {
            expandedSegmentEffortDetail.value =
                ExpandedSegmentEffortDetail(effortIndex, effortExternalId, StravaEffortDetailUiState.Loaded(cachedDetail.toItem()))
            // Retrofits effort detail that was cached before pseudo-attempts existed, and is a
            // cheap idempotent no-op otherwise — see saveStravaSegmentEffortAttempt's own doc.
            viewModelScope.launch { saveStravaSegmentEffortAttempt(rideId, effort, cachedDetail) }
            return
        }
        expandedSegmentEffortDetail.value =
            ExpandedSegmentEffortDetail(effortIndex, effortExternalId, StravaEffortDetailUiState.Loading)
        viewModelScope.launch {
            val result = fetchStravaSegmentEffortDetail(effortExternalId)
            result.onSuccess { detail -> effort?.let { saveStravaSegmentEffortAttempt(rideId, it, detail) } }
            // The user may have collapsed this panel or opened a different one while the fetch was in flight.
            if (expandedSegmentEffortDetail.value?.effortIndex != effortIndex) return@launch
            expandedSegmentEffortDetail.value = ExpandedSegmentEffortDetail(
                effortIndex,
                effortExternalId,
                result.fold(
                    onSuccess = { detail -> StravaEffortDetailUiState.Loaded(detail.toItem()) },
                    onFailure = { throwable ->
                        StravaEffortDetailUiState.Error(throwable.message ?: "Couldn't fetch effort detail.")
                    },
                ),
            )
        }
    }

    /** Long-pressed an effort row — enters selection mode (if not already in it) and selects it. */
    fun onEffortLongPress(effortExternalId: String) {
        selectedEffortIds.value = selectedEffortIds.value + effortExternalId
    }

    fun onEffortSelectionToggled(effortExternalId: String) {
        val current = selectedEffortIds.value
        selectedEffortIds.value = if (effortExternalId in current) current - effortExternalId else current + effortExternalId
    }

    fun onExitEffortSelectionMode() {
        selectedEffortIds.value = emptySet()
    }

    /**
     * Fetches and saves every selected effort's detail in one go, so the rider doesn't have to
     * expand each row individually just to get it onto the Segments page.
     */
    fun onFetchSelectedEffortsClick() {
        val ids = selectedEffortIds.value
        if (ids.isEmpty()) return
        isFetchingSelectedEfforts.value = true
        viewModelScope.launch {
            ids.forEach { effortExternalId ->
                val effort = latestEfforts.find { it.effortExternalId == effortExternalId } ?: return@forEach
                fetchStravaSegmentEffortDetail(effortExternalId).onSuccess { detail ->
                    saveStravaSegmentEffortAttempt(rideId, effort, detail)
                }
            }
            isFetchingSelectedEfforts.value = false
            selectedEffortIds.value = emptySet()
        }
    }

    fun onEditClick() {
        val ride = latestRide ?: return
        editRequest.value = EditRideRequest(name = ride.name, tag = ride.tag.orEmpty(), activityType = ride.activityType)
    }

    fun onDismissEdit() {
        editRequest.value = null
    }

    fun onEditNameChange(name: String) {
        editRequest.value = editRequest.value?.copy(name = name)
    }

    fun onEditTagChange(tag: String) {
        editRequest.value = editRequest.value?.copy(tag = tag)
    }

    fun onEditTagSuggestionClick(tag: String) {
        editRequest.value = editRequest.value?.copy(tag = tag)
    }

    fun onEditActivityTypeChange(activityType: ActivityType) {
        editRequest.value = editRequest.value?.copy(activityType = activityType)
    }

    fun onSaveEditClick() {
        val request = editRequest.value ?: return
        val trimmedName = request.name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            updateRide(rideId, trimmedName, request.tag, request.activityType)
            editRequest.value = null
        }
    }
}

private data class EditRideRequest(val name: String, val tag: String, val activityType: ActivityType)

private data class CoreRideDetail(
    val ride: RideDetailInfo?,
    val stravaSegmentEfforts: StravaEffortsUiState,
)

private fun List<StravaSegmentEffort>.toUiState(): StravaEffortsUiState =
    if (isEmpty()) StravaEffortsUiState.Idle else StravaEffortsUiState.Loaded(map { it.toItem() })

private fun Ride.toInfo(): RideDetailInfo = RideDetailInfo(
    name = name,
    activityType = activityType,
    source = source,
    dateLabel = startTime.toRideCardDate(),
    distanceKm = distanceMeters / 1000.0,
    durationLabel = duration.toRideClock(),
    elevationGainMeters = elevationGainMeters,
    avgSpeedKmh = averageSpeedKmh,
    elevationProfile = elevationProfile,
    isPersonalBest = isPersonalBest,
    tag = tag,
)

private fun StravaSegmentEffort.toItem(): StravaSegmentEffortItem = StravaSegmentEffortItem(
    effortExternalId = effortExternalId,
    segmentExternalId = segmentExternalId,
    segmentName = segmentName,
    elapsedTimeLabel = elapsedTime.toRideClock(),
    distanceKm = distanceMeters / 1000.0,
    komRank = komRank,
    prRank = prRank,
)

private fun StravaSegmentEffortDetail.toItem(): StravaSegmentEffortDetailItem = StravaSegmentEffortDetailItem(
    avgSpeedLabel = "%.1f km/h".format(avgSpeedKmh),
    maxSpeedLabel = "%.1f km/h".format(maxSpeedKmh),
    elevationGainLabel = "%.0f m".format(elevationGainMeters),
    avgWattsLabel = avgWatts?.let { "%.0f W".format(it) },
    avgHeartRateLabel = avgHeartRateBpm?.let { "%.0f bpm".format(it) },
    avgCadenceLabel = avgCadenceRpm?.let { "%.0f rpm".format(it) },
)
