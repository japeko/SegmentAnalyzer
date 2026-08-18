package com.segmentanalyzer.feature.importer.garmin

sealed interface GarminImportUiState {
    data object NotConnected : GarminImportUiState
    data object Idle : GarminImportUiState
    data object Importing : GarminImportUiState
    data class Result(val fetchedCount: Int, val importedCount: Int) : GarminImportUiState
    data class Error(val message: String) : GarminImportUiState
}
