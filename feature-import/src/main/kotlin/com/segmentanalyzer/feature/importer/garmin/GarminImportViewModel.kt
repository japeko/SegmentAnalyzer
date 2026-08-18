package com.segmentanalyzer.feature.importer.garmin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.domain.model.GarminConnectionState
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
    private val importGarminRides: ImportGarminRidesUseCase,
) : ViewModel() {

    /** Null means "derive from connection state"; non-null overrides it once import starts. */
    private val importState = MutableStateFlow<GarminImportUiState?>(null)

    val uiState: StateFlow<GarminImportUiState> = combine(
        observeGarminConnectionState(),
        importState,
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

    fun onImportClick() {
        importState.value = GarminImportUiState.Importing
        viewModelScope.launch {
            importState.value = importGarminRides().fold(
                onSuccess = { summary -> GarminImportUiState.Result(summary.fetchedCount, summary.importedCount) },
                onFailure = { throwable -> GarminImportUiState.Error(throwable.message ?: "Import failed.") },
            )
        }
    }
}
