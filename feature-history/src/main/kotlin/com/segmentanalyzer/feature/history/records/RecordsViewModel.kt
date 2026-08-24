package com.segmentanalyzer.feature.history.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.common.format.toRideCardDate
import com.segmentanalyzer.common.format.toRideClock
import com.segmentanalyzer.domain.model.SegmentRecord
import com.segmentanalyzer.domain.model.SummaryPeriod
import com.segmentanalyzer.domain.usecase.ObserveSegmentRecordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordsViewModel @Inject constructor(
    observeSegmentRecords: ObserveSegmentRecordsUseCase,
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(SummaryPeriod.THIS_MONTH)

    val uiState: StateFlow<RecordsUiState> = selectedPeriod
        .flatMapLatest { period -> observeSegmentRecords(period).map { summary -> period to summary } }
        .map { (period, summary) ->
            RecordsUiState(
                isLoading = false,
                selectedPeriod = period,
                newPersonalBests = summary.newPersonalBests.map { it.toListItem() },
                otherRecords = summary.otherRecords.map { it.toListItem() },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecordsUiState(),
        )

    fun onPeriodSelected(period: SummaryPeriod) {
        selectedPeriod.value = period
    }
}

private fun SegmentRecord.toListItem(): RecordListItem = RecordListItem(
    attemptId = attemptId,
    segmentId = segmentId,
    segmentName = segmentName,
    distanceKm = segmentDistanceMeters / 1000.0,
    rideName = rideName,
    rideSource = rideSource,
    dateLabel = startTime.toRideCardDate(),
    durationLabel = duration.toRideClock(),
    avgSpeedKmh = avgSpeedKmh,
)
