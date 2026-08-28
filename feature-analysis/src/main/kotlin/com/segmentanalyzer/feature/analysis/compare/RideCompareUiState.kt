package com.segmentanalyzer.feature.analysis.compare

import com.segmentanalyzer.domain.model.LatLng
import com.segmentanalyzer.domain.usecase.SpeedPoint
import com.segmentanalyzer.domain.usecase.TimeGapPoint
import com.segmentanalyzer.domain.util.SlopePoint

enum class AttemptRole { CURRENT, PERSONAL_BEST, PREVIOUS, SELECTED }

data class AttemptChip(
    val attemptId: Long,
    val role: AttemptRole,
    /** Whether this is the flat/zero-gap line the Time Gap chart and route map are drawn against — independent of [role], since the user can pick any chip as the reference without it taking over another chip's role label. */
    val isReference: Boolean,
    val dateLabel: String,
    /** "Ride 1", "Ride 2", ... — this attempt's lap number among all attempts from the same ride. */
    val lapLabel: String,
    val colorIndex: Int,
)

data class TimeGapSeriesUi(val attemptId: Long, val colorIndex: Int, val points: List<TimeGapPoint>)

data class SpeedSeriesUi(val attemptId: Long, val colorIndex: Int, val points: List<SpeedPoint>)

data class CompareStatValue(val attemptId: Long, val colorIndex: Int, val label: String, val fraction: Float, val isBest: Boolean)

data class CompareStatRow(val label: String, val values: List<CompareStatValue>)

data class AddableAttemptItem(
    val id: Long,
    val dateLabel: String,
    /** "Ride 1", "Ride 2", ... — this attempt's lap number among all attempts from the same ride. */
    val lapLabel: String,
    val statsLabel: String,
    /** "CURRENT" or "ADDED" if this attempt is already in the comparison and shown disabled; null if selectable. */
    val statusLabel: String?,
)

data class RideCompareUiState(
    val isLoading: Boolean = true,
    val segmentName: String = "",
    val routePoints: List<LatLng> = emptyList(),
    /** Gradient percent for each routePoints segment (size = routePoints.size - 1); null if unknown. */
    val gradientPercents: List<Double>? = null,
    val chips: List<AttemptChip> = emptyList(),
    val timeGapSeries: List<TimeGapSeriesUi> = emptyList(),
    val speedSeries: List<SpeedSeriesUi> = emptyList(),
    /** One line, not per-attempt — slope is a property of the route, not of any one ride's pace along it. */
    val slopePoints: List<SlopePoint> = emptyList(),
    /** True once distance-based charts (Slope/Speed/Time Gap) have real data — drives the shared distance-axis row. */
    val segmentDistanceMeters: Double = 0.0,
    val statRows: List<CompareStatRow> = emptyList(),
    val isAddSheetVisible: Boolean = false,
    val addableAttempts: List<AddableAttemptItem> = emptyList(),
    val selectedAddableId: Long? = null,
)
