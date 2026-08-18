package com.segmentanalyzer.feature.segments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.ui.SourceTag
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.Segment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentsScreen(
    uiState: SegmentsUiState,
    onSyncClick: () -> Unit,
    onGoToSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Segments") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SyncStatusBar(
                status = uiState.syncStatus,
                onSyncClick = onSyncClick,
                onGoToSettingsClick = onGoToSettingsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )

            if (uiState.segments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No segments yet. Sync your starred Strava segments to see them here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.segments, key = { it.id }) { segment -> SegmentCard(segment) }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusBar(
    status: StravaSyncStatus,
    onSyncClick: () -> Unit,
    onGoToSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (status) {
        StravaSyncStatus.NotConnected -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Connect Strava in Settings to sync segments.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onGoToSettingsClick) { Text("Go to Settings") }
        }

        StravaSyncStatus.Idle -> Button(onClick = onSyncClick, modifier = modifier) {
            Text("Sync starred segments")
        }

        StravaSyncStatus.Syncing -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(
                text = "Syncing…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is StravaSyncStatus.Result -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val alreadySynced = status.fetchedCount - status.syncedCount
            Text(
                text = "Synced ${status.syncedCount} new segment(s)." +
                    if (alreadySynced > 0) " $alreadySynced already up to date." else "",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSyncClick) { Text("Sync again") }
        }

        is StravaSyncStatus.Error -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = status.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = onSyncClick) { Text("Retry") }
        }
    }
}

@Composable
private fun SegmentCard(segment: Segment, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = segment.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                SourceTag(source = ActivitySource.STRAVA)
            }

            val place = listOfNotNull(segment.city, segment.state).joinToString(", ")
            if (place.isNotBlank()) {
                Text(
                    text = place,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = buildString {
                    append("%.1f km".format(segment.distanceMeters / 1000.0))
                    append(" · ")
                    append("%.1f%% avg".format(segment.averageGradePercent))
                    append(" · ")
                    append("%.1f%% max".format(segment.maximumGradePercent))
                    climbCategoryLabel(segment.climbCategory)?.let { append(" · $it") }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Strava's climb_category is 0 (uncategorized) or 1..5, where 5 is hors catégorie (hardest). */
private fun climbCategoryLabel(climbCategory: Int): String? = when (climbCategory) {
    1 -> "Cat 4"
    2 -> "Cat 3"
    3 -> "Cat 2"
    4 -> "Cat 1"
    5 -> "HC"
    else -> null
}
