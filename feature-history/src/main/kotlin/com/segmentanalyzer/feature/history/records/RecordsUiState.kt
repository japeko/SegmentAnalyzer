package com.segmentanalyzer.feature.history.records

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.SummaryPeriod

data class RecordsUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: SummaryPeriod = SummaryPeriod.THIS_MONTH,
    /** Segment records set within [selectedPeriod]. */
    val newPersonalBests: List<RecordListItem> = emptyList(),
    /** Segment records set before [selectedPeriod]. */
    val otherRecords: List<RecordListItem> = emptyList(),
    /** Non-empty means selection mode is active — keyed by attemptId. */
    val selectedAttemptIds: Set<Long> = emptySet(),
    val isExporting: Boolean = false,
    /** Non-null right after an export completes with at least one record skipped (no recorded track) — auto-dismisses itself after a delay. */
    val exportSkippedMessage: String? = null,
)

/** A segment record pre-formatted for display — the ViewModel does the formatting, not the composable. */
data class RecordListItem(
    val attemptId: Long,
    val segmentId: Long,
    val segmentName: String,
    val distanceKm: Double,
    val rideName: String,
    val rideSource: ActivitySource,
    val dateLabel: String,
    val durationLabel: String,
    val avgSpeedKmh: Double,
)
