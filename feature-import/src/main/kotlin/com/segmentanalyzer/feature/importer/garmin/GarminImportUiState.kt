package com.segmentanalyzer.feature.importer.garmin

import com.segmentanalyzer.domain.model.ActivityType
import java.time.LocalDate

sealed interface GarminImportUiState {
    data object NotConnected : GarminImportUiState

    /** [dateFrom]/[dateTo] are the rider's chosen search range — either or both may be unset. */
    data class Idle(val dateFrom: LocalDate?, val dateTo: LocalDate?) : GarminImportUiState

    data object FetchingRides : GarminImportUiState

    /** The rider is choosing which of [candidates] (by [GarminRideCandidateItem.externalId]) to import. */
    data class SelectingRides(
        val candidates: List<GarminRideCandidateItem>,
        val selectedExternalIds: Set<String>,
        val dateFrom: LocalDate?,
        val dateTo: LocalDate?,
    ) : GarminImportUiState

    data object Importing : GarminImportUiState
    data class Result(val selectedCount: Int, val importedCount: Int) : GarminImportUiState
    data class Error(val message: String) : GarminImportUiState
}

/** One Garmin ride candidate for the import picker, pre-formatted for display. */
data class GarminRideCandidateItem(
    val externalId: String,
    val name: String,
    val activityType: ActivityType,
    val dateLabel: String,
    val distanceKm: Double,
    val durationLabel: String,
)
