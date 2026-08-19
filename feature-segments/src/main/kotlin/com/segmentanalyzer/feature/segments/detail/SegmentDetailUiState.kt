package com.segmentanalyzer.feature.segments.detail

import com.segmentanalyzer.domain.model.LatLng
import com.segmentanalyzer.domain.model.Segment

data class SegmentDetailUiState(
    val isLoading: Boolean = true,
    val segment: Segment? = null,
    /** The segment's route, decoded from Strava's polyline if available, else just [start, end]. */
    val routePoints: List<LatLng> = emptyList(),
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
