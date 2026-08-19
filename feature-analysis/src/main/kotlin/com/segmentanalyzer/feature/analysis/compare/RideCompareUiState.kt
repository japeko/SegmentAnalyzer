package com.segmentanalyzer.feature.analysis.compare

import com.segmentanalyzer.domain.model.LatLng
import com.segmentanalyzer.domain.usecase.TimeGapPoint

enum class AttemptRole { CURRENT, PERSONAL_BEST, PREVIOUS, SELECTED }

data class AttemptChip(val attemptId: Long, val role: AttemptRole, val dateLabel: String, val colorIndex: Int)

data class TimeGapSeriesUi(val attemptId: Long, val colorIndex: Int, val points: List<TimeGapPoint>)

data class CompareStatValue(val attemptId: Long, val colorIndex: Int, val label: String, val fraction: Float, val isBest: Boolean)

data class CompareStatRow(val label: String, val values: List<CompareStatValue>)

data class AddableAttemptItem(
    val id: Long,
    val dateLabel: String,
    val statsLabel: String,
    /** "CURRENT" or "ADDED" if this attempt is already in the comparison and shown disabled; null if selectable. */
    val statusLabel: String?,
)

data class RideCompareUiState(
    val isLoading: Boolean = true,
    val segmentName: String = "",
    val routePoints: List<LatLng> = emptyList(),
    val chips: List<AttemptChip> = emptyList(),
    val timeGapSeries: List<TimeGapSeriesUi> = emptyList(),
    val statRows: List<CompareStatRow> = emptyList(),
    val isAddSheetVisible: Boolean = false,
    val addableAttempts: List<AddableAttemptItem> = emptyList(),
    val selectedAddableId: Long? = null,
)
