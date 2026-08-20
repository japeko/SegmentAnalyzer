package com.segmentanalyzer.feature.history.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.core.ui.ElevationSparkline
import com.segmentanalyzer.core.ui.SourceTag
import com.segmentanalyzer.core.ui.StatCard
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.feature.history.detail.components.MatchedSegmentRow
import com.segmentanalyzer.feature.history.detail.components.StravaSegmentEffortRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    uiState: RideDetailUiState,
    onBackClick: () -> Unit,
    onSegmentClick: (Long) -> Unit,
    onFetchStravaSegmentsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.ride?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.ride?.isPersonalBest == true) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = "Personal best",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val ride = uiState.ride
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (ride != null) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, end = 16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Terrain,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.height(12.dp),
                        )
                        Text(
                            text = " ${ride.activityType.label()} · ${ride.dateLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialThemeExtras.textTertiary,
                            modifier = Modifier.weight(1f),
                        )
                        SourceTag(source = ride.source)
                    }
                }
                item { StatsRow(ride = ride) }
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                        ElevationSparkline(profile = ride.elevationProfile)
                    }
                }
            }

            item {
                Text(
                    text = "Segments in this Ride",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialThemeExtras.textTertiary,
                    modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 10.dp),
                )
            }

            when {
                !uiState.hasTrack -> item { EmptyMessage("No GPS track for this ride — segment matching needs a FIT or GPX import.") }
                uiState.matchedSegments.isEmpty() -> item { EmptyMessage("No known segments matched this ride yet.") }
                else -> items(uiState.matchedSegments, key = { it.attemptId }) { match ->
                    MatchedSegmentRow(
                        item = match,
                        onClick = onSegmentClick,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            item {
                Text(
                    text = "Strava Segment Times",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialThemeExtras.textTertiary,
                    modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 10.dp),
                )
            }
            stravaEffortsSection(uiState.stravaSegmentEfforts, onFetchStravaSegmentsClick)
        }
    }
}

private fun LazyListScope.stravaEffortsSection(
    state: StravaEffortsUiState,
    onFetchClick: () -> Unit,
) {
    when (state) {
        StravaEffortsUiState.Idle -> item {
            OutlinedButton(onClick = onFetchClick, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Get Strava segment data")
            }
        }

        StravaEffortsUiState.Loading -> item {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is StravaEffortsUiState.Error -> item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = onFetchClick, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Retry")
                }
            }
        }

        is StravaEffortsUiState.Loaded -> if (state.efforts.isEmpty()) {
            item { EmptyMessage("No matching Strava activity found for this ride.") }
        } else {
            items(state.efforts) { effort ->
                StravaSegmentEffortRow(item = effort, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun StatsRow(ride: RideDetailInfo, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatCard(label = "DISTANCE", value = "%.1f km".format(ride.distanceKm))
        StatCard(label = "DURATION", value = ride.durationLabel)
        StatCard(label = "ELEV. GAIN", value = "%.0f m".format(ride.elevationGainMeters))
        StatCard(label = "AVG SPEED", value = "%.1f km/h".format(ride.avgSpeedKmh))
    }
}

@Composable
private fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun ActivityType.label(): String = when (this) {
    ActivityType.MTB -> "MTB"
    ActivityType.GRAVEL -> "Gravel"
    ActivityType.ROAD -> "Road"
    ActivityType.OTHER -> "Other"
}
