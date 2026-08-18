package com.segmentanalyzer.feature.importer.fit

sealed interface FitImportUiState {
    data object Idle : FitImportUiState
    data object Importing : FitImportUiState
    data class Result(val rideName: String, val distanceKm: Double, val elevationGainMeters: Double) : FitImportUiState
    data class Error(val message: String) : FitImportUiState
}
