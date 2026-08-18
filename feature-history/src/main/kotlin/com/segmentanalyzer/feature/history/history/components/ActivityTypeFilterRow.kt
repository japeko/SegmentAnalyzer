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
import com.segmentanalyzer.domain.model.ActivityType

private val filters: List<ActivityType?> = listOf(null, ActivityType.MTB, ActivityType.GRAVEL, ActivityType.ROAD)

private fun ActivityType?.label(): String = when (this) {
    null -> "All"
    ActivityType.MTB -> "MTB"
    ActivityType.GRAVEL -> "Gravel"
    ActivityType.ROAD -> "Road"
    ActivityType.OTHER -> "Other"
}

@Composable
fun ActivityTypeFilterRow(
    selected: ActivityType?,
    onFilterSelected: (ActivityType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label()) },
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}
