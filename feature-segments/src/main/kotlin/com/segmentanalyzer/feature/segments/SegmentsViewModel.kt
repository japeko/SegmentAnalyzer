package com.segmentanalyzer.feature.segments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.domain.model.StravaConnectionState
import com.segmentanalyzer.domain.usecase.ObserveRideTagsUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentsUseCase
import com.segmentanalyzer.domain.usecase.ObserveStravaConnectionStateUseCase
import com.segmentanalyzer.domain.usecase.SyncStravaSegmentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SegmentsViewModel @Inject constructor(
    private val observeSegments: ObserveSegmentsUseCase,
    observeStravaConnectionState: ObserveStravaConnectionStateUseCase,
    observeRideTags: ObserveRideTagsUseCase,
    private val syncStravaSegments: SyncStravaSegmentsUseCase,
) : ViewModel() {

    /** Null means "derive from connection state"; non-null overrides it once a sync starts. */
    private val syncOverride = MutableStateFlow<StravaSyncStatus?>(null)

    private val selectedTag = MutableStateFlow<String?>(null)
    private val dateFrom = MutableStateFlow<LocalDate?>(null)
    private val dateTo = MutableStateFlow<LocalDate?>(null)
    private val isFilterSheetVisible = MutableStateFlow(false)

    /**
     * The filter selection and the segments it resolves to, bundled as one emission — so a UI
     * state never shows a filter (e.g. [FilterSelection.tag]) alongside segments that don't
     * actually match it yet. Combining the two as *separate* flows (both derived from the same
     * [selectedTag]/[dateFrom]/[dateTo]) let them reach [uiState] out of step with each other,
     * since [filteredSegments] needs an extra suspend hop through [flatMapLatest] that
     * [FilterSelection] alone doesn't.
     */
    private val filteredSegments = combine(selectedTag, dateFrom, dateTo) { tag, from, to -> FilterSelection(tag, from, to) }
        .flatMapLatest { filter ->
            observeSegments(
                tag = filter.tag,
                afterEpochMillis = filter.from?.startOfDayEpochMillis(),
                // Exclusive upper bound: the day after "to", so the whole "to" day is included.
                beforeEpochMillis = filter.to?.plusDays(1)?.startOfDayEpochMillis(),
            ).map { segments -> filter to segments }
        }

    val uiState: StateFlow<SegmentsUiState> = combine(
        filteredSegments,
        observeStravaConnectionState(),
        syncOverride,
        observeRideTags(),
        isFilterSheetVisible,
    ) { (filter, segments), connectionState, override, tags, sheetVisible ->
        val status = override ?: if (connectionState is StravaConnectionState.Connected) {
            StravaSyncStatus.Idle
        } else {
            StravaSyncStatus.NotConnected
        }
        SegmentsUiState(
            segments = segments,
            syncStatus = status,
            availableTags = tags,
            selectedTag = filter.tag,
            dateFrom = filter.from,
            dateTo = filter.to,
            isFilterSheetVisible = sheetVisible,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SegmentsUiState(),
    )

    fun onSyncClick() {
        syncOverride.value = StravaSyncStatus.Syncing
        viewModelScope.launch {
            syncOverride.value = syncStravaSegments().fold(
                onSuccess = { summary -> StravaSyncStatus.Result(summary.fetchedCount, summary.syncedCount) },
                onFailure = { throwable -> StravaSyncStatus.Error(throwable.message ?: "Sync failed.") },
            )
        }
    }

    fun onFilterClick() {
        isFilterSheetVisible.value = true
    }

    fun onDismissFilterSheet() {
        isFilterSheetVisible.value = false
    }

    fun onTagSelected(tag: String?) {
        selectedTag.value = tag
    }

    fun onDateFromSelected(date: LocalDate?) {
        dateFrom.value = date
    }

    fun onDateToSelected(date: LocalDate?) {
        dateTo.value = date
    }

    fun onClearFiltersClick() {
        selectedTag.value = null
        dateFrom.value = null
        dateTo.value = null
    }
}

private data class FilterSelection(val tag: String?, val from: LocalDate?, val to: LocalDate?)

private fun LocalDate.startOfDayEpochMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
