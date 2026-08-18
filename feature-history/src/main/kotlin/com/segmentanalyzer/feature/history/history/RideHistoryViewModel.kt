package com.segmentanalyzer.feature.history.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.common.format.toRideCardDate
import com.segmentanalyzer.common.format.toRideClock
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.usecase.ObserveMonthlySummaryUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RideHistoryViewModel @Inject constructor(
    observeRideHistory: ObserveRideHistoryUseCase,
    observeMonthlySummary: ObserveMonthlySummaryUseCase,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<ActivityType?>(null)

    val uiState: StateFlow<RideHistoryUiState> = combine(
        selectedFilter.flatMapLatest { observeRideHistory(it) },
        observeMonthlySummary(),
        selectedFilter,
    ) { rides, summary, filter ->
        RideHistoryUiState(
            isLoading = false,
            monthSummary = summary,
            selectedFilter = filter,
            rides = rides.map { it.toListItem() },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RideHistoryUiState(),
    )

    fun onFilterSelected(type: ActivityType?) {
        selectedFilter.value = type
    }
}

private fun Ride.toListItem(): RideListItem = RideListItem(
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
)
