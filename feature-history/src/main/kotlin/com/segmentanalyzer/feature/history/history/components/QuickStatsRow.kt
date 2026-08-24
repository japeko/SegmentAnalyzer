package com.segmentanalyzer.feature.history.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.ui.StatCard
import com.segmentanalyzer.domain.model.SummaryPeriod
import com.segmentanalyzer.domain.usecase.RideSummary
import kotlin.math.roundToInt

@Composable
fun QuickStatsRow(summary: RideSummary?,
                  period: SummaryPeriod,
                  modifier: Modifier = Modifier,
                  onNewPBsClick: () -> Unit = {},) {
    if (summary == null) return

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        item {
            StatCard(
                label = period.statLabel(),
                value = "%.1f km".format(summary.totalDistanceKm),
                caption = "${summary.rideCount} rides · ${summary.elevationGainMeters.roundToInt()} m gain",
            )
        }
        item {
            StatCard(
                label = "New PBs",
                value = summary.newPersonalBestCount.toString(),
                caption = "personal bests ${period.captionSuffix()}",
                accented = true,
                onClick = onNewPBsClick,
            )
        }
    }
}

private fun SummaryPeriod.statLabel(): String = when (this) {
    SummaryPeriod.THIS_WEEK -> "This Week"
    SummaryPeriod.THIS_MONTH -> "This Month"
    SummaryPeriod.THIS_YEAR -> "This Year"
    SummaryPeriod.ALL_TIME -> "All Time"
}

private fun SummaryPeriod.captionSuffix(): String = when (this) {
    SummaryPeriod.THIS_WEEK -> "this week"
    SummaryPeriod.THIS_MONTH -> "this month"
    SummaryPeriod.THIS_YEAR -> "this year"
    SummaryPeriod.ALL_TIME -> "all time"
}
