package com.segmentanalyzer.feature.importer.garmin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.common.format.toRideCardDate
import com.segmentanalyzer.common.format.toRideClock
import com.segmentanalyzer.domain.model.GarminConnectionState
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.usecase.FetchGarminRidesUseCase
import com.segmentanalyzer.domain.usecase.ImportGarminRidesUseCase
import com.segmentanalyzer.domain.usecase.ObserveGarminConnectionStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class GarminImportViewModel @Inject constructor(
    observeGarminConnectionState: ObserveGarminConnectionStateUseCase,
    private val fetchGarminRides: FetchGarminRidesUseCase,
    private val importGarminRides: ImportGarminRidesUseCase,
) : ViewModel() {

    /** Null means "derive from connection state"; non-null overrides it once browsing/import starts. */
    private val screenState = MutableStateFlow<GarminImportUiState?>(null)

    /** The rider's chosen search range for the next browse — survives across a browse/import cycle. */
    private val dateFrom = MutableStateFlow<LocalDate?>(null)
    private val dateTo = MutableStateFlow<LocalDate?>(null)

    /** Narrows [GarminImportUiState.SelectingRides] to candidates whose name contains this text. */
    private val nameFilter = MutableStateFlow("")

    /** The full candidate list from the last fetch, so the import step can resolve selected ids back to [Ride]s without re-fetching. */
    private var latestCandidates: List<Ride> = emptyList()

    val uiState: StateFlow<GarminImportUiState> = combine(
        observeGarminConnectionState(),
        screenState,
        dateFrom,
        dateTo,
        nameFilter,
    ) { connectionState, override, from, to, filter ->
        val base = override ?: if (connectionState is GarminConnectionState.Connected) {
            GarminImportUiState.Idle(dateFrom = from, dateTo = to)
        } else {
            GarminImportUiState.NotConnected
        }
        if (base is GarminImportUiState.SelectingRides) base.copy(nameFilter = filter) else base
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GarminImportUiState.NotConnected,
    )

    fun onDateFromSelected(date: LocalDate?) {
        dateFrom.value = date
    }

    fun onDateToSelected(date: LocalDate?) {
        dateTo.value = date
    }

    fun onBrowseRidesClick() {
        val from = dateFrom.value
        val to = dateTo.value
        nameFilter.value = ""
        screenState.value = GarminImportUiState.FetchingRides
        viewModelScope.launch {
            screenState.value = fetchGarminRides(from, to).fold(
                onSuccess = { rides ->
                    latestCandidates = rides
                    GarminImportUiState.SelectingRides(
                        candidates = rides.map { it.toCandidateItem() },
                        selectedExternalIds = rides.mapNotNull { it.externalId }.toSet(),
                        dateFrom = from,
                        dateTo = to,
                    )
                },
                onFailure = { throwable -> GarminImportUiState.Error(throwable.message ?: "Couldn't fetch rides.") },
            )
        }
    }

    fun onRideToggled(externalId: String) {
        val state = screenState.value as? GarminImportUiState.SelectingRides ?: return
        val selected = state.selectedExternalIds
        screenState.value = state.copy(
            selectedExternalIds = if (externalId in selected) selected - externalId else selected + externalId,
        )
    }

    fun onNameFilterChange(filter: String) {
        nameFilter.value = filter
    }

    /** Toggles selection for whatever [GarminImportUiState.SelectingRides.visibleCandidates] currently shows, not the full candidate list. */
    fun onSelectAllToggled() {
        val state = (screenState.value as? GarminImportUiState.SelectingRides)?.copy(nameFilter = nameFilter.value) ?: return
        val visibleIds = state.visibleCandidates.map { it.externalId }.toSet()
        val allVisibleSelected = visibleIds.isNotEmpty() && state.selectedExternalIds.containsAll(visibleIds)
        screenState.value = state.copy(
            selectedExternalIds = if (allVisibleSelected) state.selectedExternalIds - visibleIds else state.selectedExternalIds + visibleIds,
        )
    }

    fun onImportSelectedClick() {
        val state = screenState.value as? GarminImportUiState.SelectingRides ?: return
        val selectedRides = latestCandidates.filter { it.externalId in state.selectedExternalIds }
        screenState.value = GarminImportUiState.Importing
        viewModelScope.launch {
            screenState.value = importGarminRides(selectedRides).fold(
                onSuccess = { summary -> GarminImportUiState.Result(summary.selectedCount, summary.importedCount) },
                onFailure = { throwable -> GarminImportUiState.Error(throwable.message ?: "Import failed.") },
            )
        }
    }

    /** Drops any override so [uiState] falls back to [GarminImportUiState.Idle], letting the rider adjust the date range before browsing again. */
    fun onBackToIdleClick() {
        screenState.value = null
        nameFilter.value = ""
    }
}

private fun Ride.toCandidateItem(): GarminRideCandidateItem = GarminRideCandidateItem(
    externalId = requireNotNull(externalId) { "Garmin ride candidates always have an externalId" },
    name = name,
    activityType = activityType,
    dateLabel = startTime.toRideCardDate(),
    distanceKm = distanceMeters / 1000.0,
    durationLabel = duration.toRideClock(),
)
