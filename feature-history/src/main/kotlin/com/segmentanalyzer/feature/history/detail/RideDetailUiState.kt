package com.segmentanalyzer.feature.history.detail

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType

data class RideDetailUiState(
    val isLoading: Boolean = true,
    val ride: RideDetailInfo? = null,
    /** Whether this ride has a stored GPS track — only FIT/GPX imports do. */
    val hasTrack: Boolean = false,
    val matchedSegments: List<MatchedSegmentItem> = emptyList(),
)

/** A ride pre-formatted for display — the ViewModel does the formatting, not the composable. */
data class RideDetailInfo(
    val name: String,
    val activityType: ActivityType,
    val source: ActivitySource,
    val dateLabel: String,
    val distanceKm: Double,
    val durationLabel: String,
    val elevationGainMeters: Double,
    val avgSpeedKmh: Double,
    val elevationProfile: List<Float>,
    val isPersonalBest: Boolean,
)

/** A starred Strava segment this ride passed through. */
data class MatchedSegmentItem(
    val attemptId: Long,
    val segmentId: Long,
    val name: String,
    val distanceKm: Double,
    val dateLabel: String,
    val durationLabel: String,
    val avgSpeedKmh: Double,
    val isPersonalBest: Boolean,
)
