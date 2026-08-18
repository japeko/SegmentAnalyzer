package com.segmentanalyzer.feature.history.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.ui.StatCard
import com.segmentanalyzer.domain.usecase.MonthlySummary
import kotlin.math.roundToInt

@Composable
fun QuickStatsRow(summary: MonthlySummary?, modifier: Modifier = Modifier) {
    if (summary == null) return

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        item {
            StatCard(
                label = "This Month",
                value = "%.1f km".format(summary.totalDistanceKm),
                caption = "${summary.rideCount} rides · ${summary.elevationGainMeters.roundToInt()} m gain",
            )
        }
        item {
            StatCard(
                label = "New PBs",
                value = summary.newPersonalBestCount.toString(),
                caption = "personal bests this month",
                accented = true,
            )
        }
    }
}
