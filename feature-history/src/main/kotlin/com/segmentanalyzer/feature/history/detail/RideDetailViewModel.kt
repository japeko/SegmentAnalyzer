package com.segmentanalyzer.feature.history.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.common.format.toRideCardDate
import com.segmentanalyzer.common.format.toRideClock
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.RideSegmentMatch
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.usecase.FetchStravaSegmentEffortsUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideHasTrackUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentMatchesForRideUseCase
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
    private val fetchStravaSegmentEfforts: FetchStravaSegmentEffortsUseCase,
) : ViewModel() {

    private val rideId: Long = checkNotNull(savedStateHandle["rideId"])

    /** The ride from the most recent [uiState] emission, so onFetchStravaSegmentsClick can read it without a lag-prone second subscription. */
    private var latestRide: Ride? = null
    private val stravaEffortsState = MutableStateFlow<StravaEffortsUiState>(StravaEffortsUiState.Idle)

    val uiState = combine(
        observeRide(rideId),
        observeRideHasTrack(rideId),
        observeSegmentMatchesForRide(rideId),
        stravaEffortsState,
    ) { ride, hasTrack, matches, stravaEfforts ->
        latestRide = ride
        RideDetailUiState(
            isLoading = false,
            ride = ride?.toInfo(),
            hasTrack = hasTrack,
            matchedSegments = matches.map { it.toItem() },
            stravaSegmentEfforts = stravaEfforts,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RideDetailUiState(),
    )

    fun onFetchStravaSegmentsClick() {
        val ride = latestRide ?: return
        stravaEffortsState.value = StravaEffortsUiState.Loading
        viewModelScope.launch {
            stravaEffortsState.value = fetchStravaSegmentEfforts(ride).fold(
                onSuccess = { efforts -> StravaEffortsUiState.Loaded(efforts.map { it.toItem() }) },
                onFailure = { throwable ->
                    StravaEffortsUiState.Error(throwable.message ?: "Couldn't fetch Strava segment data.")
                },
            )
        }
    }
}

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
    segmentName = segmentName,
    elapsedTimeLabel = elapsedTime.toRideClock(),
    distanceKm = distanceMeters / 1000.0,
    komRank = komRank,
    prRank = prRank,
)
