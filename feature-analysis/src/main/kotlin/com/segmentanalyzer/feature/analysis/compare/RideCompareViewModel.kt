package com.segmentanalyzer.feature.analysis.compare

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.common.format.toRideCardDate
import com.segmentanalyzer.common.format.toRideClock
import com.segmentanalyzer.domain.model.LatLng
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.usecase.BuildTimeGapSeriesUseCase
import com.segmentanalyzer.domain.usecase.GetAttemptTrackUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentAttemptsUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentsUseCase
import com.segmentanalyzer.domain.util.gradientPercentSegments
import com.segmentanalyzer.domain.util.lapLabelsByAttemptId
import com.segmentanalyzer.domain.util.routePoints
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class AddSheetState(val isVisible: Boolean = false, val selectedAddableId: Long? = null)

@HiltViewModel
class RideCompareViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeSegments: ObserveSegmentsUseCase,
    observeSegmentAttempts: ObserveSegmentAttemptsUseCase,
    private val buildTimeGapSeries: BuildTimeGapSeriesUseCase,
    private val getAttemptTrack: GetAttemptTrackUseCase,
) : ViewModel() {

    private val segmentId: Long = checkNotNull(savedStateHandle["segmentId"])
    private val currentAttemptId: Long = checkNotNull(savedStateHandle["anchorAttemptId"])

    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val excludedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val addSheetState = MutableStateFlow(AddSheetState())
    // Current's actual GPS track (with elevation, if the source ride had it) — used to draw the
    // map's route gradient-colored by slope. Falls back to the segment's flat polyline if empty
    // (e.g. a Garmin- or Strava-sourced ride, which has no stored track in V1).
    private val currentTrack = MutableStateFlow<List<TrackPoint>>(emptyList())

    init {
        viewModelScope.launch {
            currentTrack.value = getAttemptTrack(currentAttemptId)
        }
    }

    val uiState: StateFlow<RideCompareUiState> = combine(
        observeSegments().map { segments -> segments.find { it.id == segmentId } },
        observeSegmentAttempts(segmentId),
        selectedIds,
        excludedIds,
        combine(addSheetState, currentTrack) { sheet, track -> sheet to track },
    ) { segment, attempts, selected, excluded, (sheet, track) ->
        if (segment == null || attempts.isEmpty()) {
            return@combine RideCompareUiState(isLoading = true)
        }

        val current = attempts.find { it.id == currentAttemptId }
        val personalBest = attempts.minByOrNull { it.duration.seconds }
        val previous = attempts
            .filter { it.id != currentAttemptId && current != null && it.startTime.isBefore(current.startTime) }
            .maxByOrNull { it.startTime }

        fun roleFor(id: Long): AttemptRole = when (id) {
            current?.id -> AttemptRole.CURRENT
            personalBest?.id -> AttemptRole.PERSONAL_BEST
            previous?.id -> AttemptRole.PREVIOUS
            else -> AttemptRole.SELECTED
        }

        // Current is always shown — it's the screen's anchor. Personal Best/Previous are only
        // defaults; the user can dismiss either one (onRemoveAttempt), and re-add it later via
        // the picker if they want it back.
        val defaultIds = listOfNotNull(
            current?.id,
            personalBest?.id?.takeIf { it != current?.id && it !in excluded },
            previous?.id?.takeIf { it != current?.id && it != personalBest?.id && it !in excluded },
        )
        val orderedIds = (defaultIds + selected.filter { it !in defaultIds }).distinct()
        val lapLabels = lapLabelsByAttemptId(attempts)

        val chips = orderedIds.mapIndexedNotNull { index, id ->
            attempts.find { it.id == id }?.let { attempt ->
                AttemptChip(
                    attemptId = id,
                    role = roleFor(id),
                    dateLabel = attempt.startTime.toRideCardDate(),
                    lapLabel = lapLabels.getValue(id),
                    colorIndex = index,
                )
            }
        }

        val otherIds = chips.map { it.attemptId }.filter { it != currentAttemptId }
        val timeGapSeries = if (current != null && otherIds.isNotEmpty()) {
            val colorByAttemptId = chips.associate { it.attemptId to it.colorIndex }
            buildTimeGapSeries(currentAttemptId, otherIds, segment.distanceMeters).map { series ->
                TimeGapSeriesUi(
                    attemptId = series.attemptId,
                    colorIndex = colorByAttemptId[series.attemptId] ?: 0,
                    points = series.points,
                )
            }
        } else {
            emptyList()
        }

        val addable = attempts.map { attempt ->
            AddableAttemptItem(
                id = attempt.id,
                dateLabel = attempt.startTime.toRideCardDate(),
                lapLabel = lapLabels.getValue(attempt.id),
                statsLabel = "${attempt.duration.toRideClock()} · %.1f km/h".format(attempt.avgSpeedKmh),
                statusLabel = when {
                    attempt.id == currentAttemptId -> "CURRENT · ${lapLabels.getValue(attempt.id)}"
                    attempt.id in orderedIds -> "ADDED · ${lapLabels.getValue(attempt.id)}"
                    else -> null
                },
            )
        }

        RideCompareUiState(
            isLoading = false,
            segmentName = segment.name,
            routePoints = if (track.isNotEmpty()) track.map { LatLng(it.latitude, it.longitude) } else segment.routePoints(),
            gradientPercents = if (track.isNotEmpty()) gradientPercentSegments(track) else null,
            chips = chips,
            timeGapSeries = timeGapSeries,
            statRows = buildStatRows(chips, attempts),
            isAddSheetVisible = sheet.isVisible,
            addableAttempts = addable,
            selectedAddableId = sheet.selectedAddableId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RideCompareUiState(),
    )

    fun onAddClick() {
        addSheetState.value = AddSheetState(isVisible = true)
    }

    fun onDismissAddSheet() {
        addSheetState.value = AddSheetState()
    }

    fun onAddableAttemptSelected(attemptId: Long) {
        addSheetState.value = addSheetState.value.copy(selectedAddableId = attemptId)
    }

    fun onConfirmAdd() {
        addSheetState.value.selectedAddableId?.let { id -> selectedIds.value = selectedIds.value + id }
        addSheetState.value = AddSheetState()
    }

    /**
     * Removes a chip from the comparison — a manually-added one, or a Personal Best/Previous
     * default (Current can't be removed; it's the screen's anchor and the UI never offers it).
     * Excluding a default's id keeps it from being auto-picked again; re-adding it via the picker
     * still works, since [selectedIds] is checked independently of [excludedIds].
     */
    fun onRemoveAttempt(attemptId: Long) {
        selectedIds.value = selectedIds.value - attemptId
        excludedIds.value = excludedIds.value + attemptId
    }
}

private fun buildStatRows(chips: List<AttemptChip>, attempts: List<SegmentAttempt>): List<CompareStatRow> {
    val chipAttempts = chips.mapNotNull { chip -> attempts.find { it.id == chip.attemptId }?.let { chip to it } }
    if (chipAttempts.isEmpty()) return emptyList()

    val rows = mutableListOf<CompareStatRow>()

    val maxDuration = chipAttempts.maxOf { it.second.duration.seconds }.toDouble()
    val minDuration = chipAttempts.minOf { it.second.duration.seconds }
    rows += CompareStatRow(
        label = "Total Time",
        values = chipAttempts.map { (chip, attempt) ->
            CompareStatValue(
                attemptId = chip.attemptId,
                colorIndex = chip.colorIndex,
                label = attempt.duration.toRideClock(),
                fraction = fraction(attempt.duration.seconds.toDouble(), maxDuration),
                isBest = attempt.duration.seconds == minDuration,
            )
        },
    )

    val maxSpeed = chipAttempts.maxOf { it.second.avgSpeedKmh }
    rows += CompareStatRow(
        label = "Avg Speed",
        values = chipAttempts.map { (chip, attempt) ->
            CompareStatValue(
                attemptId = chip.attemptId,
                colorIndex = chip.colorIndex,
                label = "%.1f km/h".format(attempt.avgSpeedKmh),
                fraction = fraction(attempt.avgSpeedKmh, maxSpeed),
                isBest = attempt.avgSpeedKmh == maxSpeed,
            )
        },
    )

    val maxGain = chipAttempts.maxOf { it.second.elevationGainMeters }
    rows += CompareStatRow(
        label = "Elevation Gain",
        // No "best" checkmark here — every attempt covers the same fixed route, so differences
        // mostly reflect GPS noise rather than a meaningful performance signal.
        values = chipAttempts.map { (chip, attempt) ->
            CompareStatValue(
                attemptId = chip.attemptId,
                colorIndex = chip.colorIndex,
                label = "%.0f m".format(attempt.elevationGainMeters),
                fraction = fraction(attempt.elevationGainMeters, maxGain),
                isBest = false,
            )
        },
    )

    val powers = chipAttempts.mapNotNull { it.second.avgPowerWatts }
    if (powers.isNotEmpty()) {
        val maxPower = powers.max()
        rows += CompareStatRow(
            label = "Avg Power",
            values = chipAttempts.mapNotNull { (chip, attempt) ->
                attempt.avgPowerWatts?.let { power ->
                    CompareStatValue(
                        attemptId = chip.attemptId,
                        colorIndex = chip.colorIndex,
                        label = "%.0f W".format(power),
                        fraction = fraction(power, maxPower),
                        isBest = power == maxPower,
                    )
                }
            },
        )
    }

    return rows
}

private fun fraction(value: Double, max: Double): Float = if (max <= 0.0) 0f else (value / max).toFloat()
