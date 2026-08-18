package com.segmentanalyzer.feature.history.history

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.usecase.MonthlySummary

data class RideHistoryUiState(
    val isLoading: Boolean = true,
    val monthSummary: MonthlySummary? = null,
    val selectedFilter: ActivityType? = null,
    val rides: List<RideListItem> = emptyList(),
)

/** A ride pre-formatted for display — the ViewModel does the formatting, not the composable. */
data class RideListItem(
    val id: Long,
    val name: String,
    val activityType: ActivityType,
    val source: ActivitySource,
    val dateLabel: String,
    val distanceKm: Double,
    val durationLabel: String,
    val elevationGainMeters: Double,
    val avgSpeedKmh: Double,
    val isPersonalBest: Boolean,
    val elevationProfile: List<Float>,
)
