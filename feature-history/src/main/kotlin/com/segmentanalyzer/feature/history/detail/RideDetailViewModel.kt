package com.segmentanalyzer.feature.history.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.common.format.toRideCardDate
import com.segmentanalyzer.common.format.toRideClock
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.RideSegmentMatch
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.model.StravaSegmentEffortDetail
import com.segmentanalyzer.domain.usecase.FetchStravaSegmentEffortDetailUseCase
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
    private val fetchStravaSegmentEffortDetail: FetchStravaSegmentEffortDetailUseCase,
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

    /** Which effort's full pace/power/HR detail is expanded, and its fetch state. Null = none expanded. */
    private val expandedSegmentEffortDetail = MutableStateFlow<ExpandedSegmentEffortDetail?>(null)

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

    val uiState = combine(coreState, expandedSegmentEffortDetail) { core, expandedDetail ->
        RideDetailUiState(
            isLoading = false,
            ride = core.ride,
            hasTrack = core.hasTrack,
            matchedSegments = core.matchedSegments,
            stravaSegmentEfforts = core.stravaSegmentEfforts,
            expandedSegmentEffortDetail = expandedDetail,
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
     * Toggles the pace/power/HR detail panel for one segment-effort row (identified by
     * [effortIndex], since a ride can pass through the same segment more than once): expands and
     * fetches it, or collapses it if already open.
     */
    fun onStravaSegmentEffortClick(effortIndex: Int, effortExternalId: String) {
        if (expandedSegmentEffortDetail.value?.effortIndex == effortIndex) {
            expandedSegmentEffortDetail.value = null
            return
        }
        expandedSegmentEffortDetail.value =
            ExpandedSegmentEffortDetail(effortIndex, effortExternalId, StravaEffortDetailUiState.Loading)
        viewModelScope.launch {
            val result = fetchStravaSegmentEffortDetail(effortExternalId)
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
