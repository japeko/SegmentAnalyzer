package com.segmentanalyzer.feature.importer.gpx

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.domain.usecase.ImportGpxFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GpxFileImportViewModel @Inject constructor(
    private val importGpxFile: ImportGpxFileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GpxImportUiState>(GpxImportUiState.Idle)
    val uiState: StateFlow<GpxImportUiState> = _uiState.asStateFlow()

    fun onFileSelected(uri: String) {
        _uiState.value = GpxImportUiState.Importing
        viewModelScope.launch {
            _uiState.value = importGpxFile(uri).fold(
                onSuccess = { ride ->
                    GpxImportUiState.Result(
                        rideName = ride.name,
                        distanceKm = ride.distanceMeters / 1000.0,
                        elevationGainMeters = ride.elevationGainMeters,
                    )
                },
                onFailure = { throwable ->
                    GpxImportUiState.Error(throwable.message ?: "Couldn't import that file.")
                },
            )
        }
    }
}
