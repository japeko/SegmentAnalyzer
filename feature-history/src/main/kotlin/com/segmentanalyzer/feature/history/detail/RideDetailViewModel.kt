package com.segmentanalyzer.feature.history.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.common.format.toRideCardDate
import com.segmentanalyzer.common.format.toRideClock
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.RideSegmentMatch
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.model.StravaSegmentEffortHistoryEntry
import com.segmentanalyzer.domain.usecase.FetchStravaSegmentEffortHistoryUseCase
import com.segmentanalyzer.domain.usecase.FetchStravaSegmentEffortsUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideHasTrackUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentMatchesForRideUseCase
import com.segmentanalyzer.domain.usecase.ObserveStravaSegmentEffortsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RideDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeRide: ObserveRideUseCase,
    observeRideHasTrack: ObserveRideHasTrackUseCase,
    observeSegmentMatchesForRide: ObserveSegmentMatchesForRideUseCase,
    observeStravaSegmentEfforts: ObserveStravaSegmentEffortsUseCase,
    private val fetchStravaSegmentEfforts: FetchStravaSegmentEffortsUseCase,
    private val fetchStravaSegmentEffortHistory: FetchStravaSegmentEffortHistoryUseCase,
) : ViewModel() {

    private val rideId: Long = checkNotNull(savedStateHandle["rideId"])

    /** The ride from the most recent [uiState] emission, so onFetchStravaSegmentsClick can read it without a lag-prone second subscription. */
    private var latestRide: Ride? = null

    /**
     * Overrides the cache-derived Strava state once a fetch has been triggered in this
     * ViewModel instance (Loading/Loaded/Error). Null means "show whatever's cached," so a ride
     * with previously-fetched data opens already populated instead of forcing another fetch.
     */
    private val stravaEffortsOverride = MutableStateFlow<StravaEffortsUiState?>(null)

    /** Which segment's full Strava effort history is expanded, and its fetch state. Null = none expanded. */
    private val expandedSegmentEffortHistory = MutableStateFlow<ExpandedSegmentEffortHistory?>(null)

    private val coreState = combine(
        observeRide(rideId),
        observeRideHasTrack(rideId),
        observeSegmentMatchesForRide(rideId),
        observeStravaSegmentEfforts(rideId),
        stravaEffortsOverride,
    ) { ride, hasTrack, matches, cachedEfforts, override ->
        latestRide = ride
        CoreRideDetail(
            ride = ride?.toInfo(),
            hasTrack = hasTrack,
            matchedSegments = matches.map { it.toItem() },
            stravaSegmentEfforts = override ?: cachedEfforts.toUiState(),
        )
    }

    val uiState = combine(coreState, expandedSegmentEffortHistory) { core, expandedHistory ->
        RideDetailUiState(
            isLoading = false,
            ride = core.ride,
            hasTrack = core.hasTrack,
            matchedSegments = core.matchedSegments,
            stravaSegmentEfforts = core.stravaSegmentEfforts,
            expandedSegmentEffortHistory = expandedHistory,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RideDetailUiState(),
    )

    fun onFetchStravaSegmentsClick() {
        val ride = latestRide ?: return
        stravaEffortsOverride.value = StravaEffortsUiState.Loading
        viewModelScope.launch {
            stravaEffortsOverride.value = fetchStravaSegmentEfforts(ride).fold(
                onSuccess = { efforts -> StravaEffortsUiState.Loaded(efforts.map { it.toItem() }) },
                onFailure = { throwable ->
                    StravaEffortsUiState.Error(throwable.message ?: "Couldn't fetch Strava segment data.")
                },
            )
        }
    }

    /**
     * Toggles the full effort-history panel for one segment-effort row (identified by
     * [effortIndex], since a ride can pass through the same segment more than once): expands and
     * fetches it, or collapses it if already open.
     */
    fun onStravaSegmentEffortClick(effortIndex: Int, segmentExternalId: String) {
        if (expandedSegmentEffortHistory.value?.effortIndex == effortIndex) {
            expandedSegmentEffortHistory.value = null
            return
        }
        expandedSegmentEffortHistory.value =
            ExpandedSegmentEffortHistory(effortIndex, segmentExternalId, StravaEffortHistoryUiState.Loading)
        viewModelScope.launch {
            val result = fetchStravaSegmentEffortHistory(segmentExternalId)
            // The user may have collapsed this panel or opened a different one while the fetch was in flight.
            if (expandedSegmentEffortHistory.value?.effortIndex != effortIndex) return@launch
            expandedSegmentEffortHistory.value = ExpandedSegmentEffortHistory(
                effortIndex,
                segmentExternalId,
                result.fold(
                    onSuccess = { entries -> StravaEffortHistoryUiState.Loaded(entries.map { it.toItem() }) },
                    onFailure = { throwable ->
                        StravaEffortHistoryUiState.Error(throwable.message ?: "Couldn't fetch segment history.")
                    },
                ),
            )
        }
    }
}

private data class CoreRideDetail(
    val ride: RideDetailInfo?,
    val hasTrack: Boolean,
    val matchedSegments: List<MatchedSegmentItem>,
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
)

private fun RideSegmentMatch.toItem(): MatchedSegmentItem = MatchedSegmentItem(
    attemptId = attemptId,
    segmentId = segmentId,
    name = segmentName,
    distanceKm = segmentDistanceMeters / 1000.0,
    dateLabel = startTime.toRideCardDate(),
    durationLabel = duration.toRideClock(),
    avgSpeedKmh = avgSpeedKmh,
    isPersonalBest = isPersonalBest,
)

private fun StravaSegmentEffort.toItem(): StravaSegmentEffortItem = StravaSegmentEffortItem(
    segmentExternalId = segmentExternalId,
    segmentName = segmentName,
    elapsedTimeLabel = elapsedTime.toRideClock(),
    distanceKm = distanceMeters / 1000.0,
    komRank = komRank,
    prRank = prRank,
)

private fun StravaSegmentEffortHistoryEntry.toItem(): StravaSegmentEffortHistoryItem = StravaSegmentEffortHistoryItem(
    dateLabel = startTime.toRideCardDate(),
    elapsedTimeLabel = elapsedTime.toRideClock(),
    distanceKm = distanceMeters / 1000.0,
    komRank = komRank,
    prRank = prRank,
)
