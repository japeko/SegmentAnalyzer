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
import javax.inject.Inject

@HiltViewModel
class GarminImportViewModel @Inject constructor(
    observeGarminConnectionState: ObserveGarminConnectionStateUseCase,
    private val fetchGarminRides: FetchGarminRidesUseCase,
    private val importGarminRides: ImportGarminRidesUseCase,
) : ViewModel() {

    /** Null means "derive from connection state"; non-null overrides it once browsing/import starts. */
    private val screenState = MutableStateFlow<GarminImportUiState?>(null)

    /** The full candidate list from the last fetch, so the import step can resolve selected ids back to [Ride]s without re-fetching. */
    private var latestCandidates: List<Ride> = emptyList()

    val uiState: StateFlow<GarminImportUiState> = combine(
        observeGarminConnectionState(),
        screenState,
    ) { connectionState, override ->
        override ?: if (connectionState is GarminConnectionState.Connected) {
            GarminImportUiState.Idle
        } else {
            GarminImportUiState.NotConnected
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GarminImportUiState.NotConnected,
    )

    fun onBrowseRidesClick() {
        screenState.value = GarminImportUiState.FetchingRides
        viewModelScope.launch {
            screenState.value = fetchGarminRides().fold(
                onSuccess = { rides ->
                    latestCandidates = rides
                    GarminImportUiState.SelectingRides(
                        candidates = rides.map { it.toCandidateItem() },
                        selectedExternalIds = rides.mapNotNull { it.externalId }.toSet(),
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

    fun onSelectAllToggled() {
        val state = screenState.value as? GarminImportUiState.SelectingRides ?: return
        val allIds = state.candidates.map { it.externalId }.toSet()
        screenState.value = state.copy(
            selectedExternalIds = if (state.selectedExternalIds.size == allIds.size) emptySet() else allIds,
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
}

private fun Ride.toCandidateItem(): GarminRideCandidateItem = GarminRideCandidateItem(
    externalId = requireNotNull(externalId) { "Garmin ride candidates always have an externalId" },
    name = name,
    activityType = activityType,
    dateLabel = startTime.toRideCardDate(),
    distanceKm = distanceMeters / 1000.0,
    durationLabel = duration.toRideClock(),
)
