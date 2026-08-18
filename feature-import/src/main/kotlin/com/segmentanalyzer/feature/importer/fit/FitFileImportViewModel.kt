package com.segmentanalyzer.feature.importer.fit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.domain.usecase.ImportFitFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FitFileImportViewModel @Inject constructor(
    private val importFitFile: ImportFitFileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<FitImportUiState>(FitImportUiState.Idle)
    val uiState: StateFlow<FitImportUiState> = _uiState.asStateFlow()

    fun onFileSelected(uri: String) {
        _uiState.value = FitImportUiState.Importing
        viewModelScope.launch {
            _uiState.value = importFitFile(uri).fold(
                onSuccess = { ride ->
                    FitImportUiState.Result(
                        rideName = ride.name,
                        distanceKm = ride.distanceMeters / 1000.0,
                        elevationGainMeters = ride.elevationGainMeters,
                    )
                },
                onFailure = { throwable ->
                    FitImportUiState.Error(throwable.message ?: "Couldn't import that file.")
                },
            )
        }
    }
}
