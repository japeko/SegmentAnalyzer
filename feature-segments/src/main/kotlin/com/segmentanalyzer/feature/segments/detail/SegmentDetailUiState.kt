package com.segmentanalyzer.feature.segments.detail

import com.segmentanalyzer.domain.model.Segment

data class SegmentDetailUiState(
    val isLoading: Boolean = true,
    val segment: Segment? = null,
    val personalBest: AttemptItem? = null,
    val personalBestDeltaSeconds: Long? = null,
    val progressPoints: List<ProgressPoint> = emptyList(),
    val attempts: List<AttemptItem> = emptyList(),
)

data class AttemptItem(
    val id: Long,
    val rideId: Long,
    val rideName: String,
    val dateLabel: String,
    val durationLabel: String,
    val deltaVsPrSeconds: Long,
    val isPersonalBest: Boolean,
)

data class ProgressPoint(val attemptId: Long, val normalizedY: Float, val isPersonalBest: Boolean)
