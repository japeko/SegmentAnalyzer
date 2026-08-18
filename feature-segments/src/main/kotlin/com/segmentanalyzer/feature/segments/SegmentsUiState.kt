package com.segmentanalyzer.feature.segments

import com.segmentanalyzer.domain.model.Segment

sealed interface StravaSyncStatus {
    data object NotConnected : StravaSyncStatus
    data object Idle : StravaSyncStatus
    data object Syncing : StravaSyncStatus
    data class Result(val fetchedCount: Int, val syncedCount: Int) : StravaSyncStatus
    data class Error(val message: String) : StravaSyncStatus
}

data class SegmentsUiState(
    val segments: List<Segment> = emptyList(),
    val syncStatus: StravaSyncStatus = StravaSyncStatus.NotConnected,
)
