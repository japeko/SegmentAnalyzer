package com.segmentanalyzer.feature.segments.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.common.format.toRideCardDate
import com.segmentanalyzer.common.format.toRideClock
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.usecase.ObserveSegmentAttemptsUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentsUseCase
import com.segmentanalyzer.domain.util.lapLabelsByAttemptId
import com.segmentanalyzer.domain.util.routePoints
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SegmentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeSegments: ObserveSegmentsUseCase,
    observeSegmentAttempts: ObserveSegmentAttemptsUseCase,
) : ViewModel() {

    private val segmentId: Long = checkNotNull(savedStateHandle["segmentId"])

    val uiState = combine(
        observeSegments().map { segments -> segments.find { it.id == segmentId } },
        observeSegmentAttempts(segmentId),
    ) { segment, attempts ->
        val sortedByDuration = attempts.sortedBy { it.duration }
        val personalBest = sortedByDuration.firstOrNull()
        val personalBestDeltaSeconds = if (sortedByDuration.size >= 2) {
            sortedByDuration[1].duration.seconds - sortedByDuration[0].duration.seconds
        } else {
            null
        }

        val chronological = attempts.sortedBy { it.startTime }
        val minSeconds = chronological.minOfOrNull { it.duration.seconds }
        val maxSeconds = chronological.maxOfOrNull { it.duration.seconds }
        val progressPoints = chronological.map { attempt ->
            ProgressPoint(
                attemptId = attempt.id,
                normalizedY = normalizedSpeed(attempt.duration.seconds, minSeconds, maxSeconds),
                isPersonalBest = attempt.id == personalBest?.id,
            )
        }

        val lapLabels = lapLabelsByAttemptId(chronological)

        SegmentDetailUiState(
            isLoading = false,
            segment = segment,
            routePoints = segment?.routePoints().orEmpty(),
            personalBest = personalBest?.toItem(personalBest.duration.seconds, personalBest.id, lapLabels.getValue(personalBest.id)),
            personalBestDeltaSeconds = personalBestDeltaSeconds,
            progressPoints = progressPoints,
            attempts = chronological.map {
                it.toItem(personalBest?.duration?.seconds ?: 0, personalBest?.id, lapLabels.getValue(it.id))
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SegmentDetailUiState(),
    )
}

/** Faster (lower seconds) plots higher on the chart — the intuitive "improving" reading. */
private fun normalizedSpeed(seconds: Long, min: Long?, max: Long?): Float {
    if (min == null || max == null || max == min) return 1f
    return 1f - (seconds - min).toFloat() / (max - min).toFloat()
}

private fun SegmentAttempt.toItem(personalBestSeconds: Long, personalBestId: Long?, lapLabel: String): AttemptItem = AttemptItem(
    id = id,
    rideId = rideId,
    rideName = rideName,
    dateLabel = startTime.toRideCardDate(),
    lapLabel = lapLabel,
    durationLabel = duration.toRideClock(),
    deltaVsPrSeconds = duration.seconds - personalBestSeconds,
    isPersonalBest = id == personalBestId,
    isFromStrava = isFromStrava,
)
