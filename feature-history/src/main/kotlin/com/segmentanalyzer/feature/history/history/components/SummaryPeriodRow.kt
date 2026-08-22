package com.segmentanalyzer.feature.history.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.domain.model.SummaryPeriod

private fun SummaryPeriod.label(): String = when (this) {
    SummaryPeriod.THIS_WEEK -> "Week"
    SummaryPeriod.THIS_MONTH -> "Month"
    SummaryPeriod.THIS_YEAR -> "Year"
    SummaryPeriod.ALL_TIME -> "All Time"
}

@Composable
fun SummaryPeriodRow(
    selected: SummaryPeriod,
    onPeriodSelected: (SummaryPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(SummaryPeriod.entries) { period ->
            FilterChip(
                selected = period == selected,
                onClick = { onPeriodSelected(period) },
                label = { Text(period.label()) },
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}
