package com.segmentanalyzer.feature.importer.gpx

sealed interface GpxImportUiState {
    data object Idle : GpxImportUiState
    data object Importing : GpxImportUiState
    data class Result(val rideName: String, val distanceKm: Double, val elevationGainMeters: Double) : GpxImportUiState
    data class Error(val message: String) : GpxImportUiState
}
