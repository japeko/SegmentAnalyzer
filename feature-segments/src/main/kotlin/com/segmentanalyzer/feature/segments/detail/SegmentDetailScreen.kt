package com.segmentanalyzer.feature.segments.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.core.ui.RoutePreviewCard
import com.segmentanalyzer.core.ui.StatCard
import com.segmentanalyzer.domain.model.Segment
import com.segmentanalyzer.feature.segments.detail.components.ExcludableAttemptRow
import com.segmentanalyzer.feature.segments.detail.components.GuestAttemptRow
import com.segmentanalyzer.feature.segments.detail.components.GuestImportSheet
import com.segmentanalyzer.feature.segments.detail.components.IncludableAttemptRow
import com.segmentanalyzer.feature.segments.detail.components.ProgressChart

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SegmentDetailScreen(
    uiState: SegmentDetailUiState,
    onBackClick: () -> Unit,
    onAttemptClick: (Long) -> Unit,
    onAttemptSelected: (Long) -> Unit,
    onAttemptExcluded: (Long) -> Unit,
    onAttemptIncluded: (Long) -> Unit,
    onToggleAttemptsOrder: () -> Unit,
    onStarSegmentClick: () -> Unit,
    onDismissStarPrompt: () -> Unit,
    onGoToSettingsClick: () -> Unit,
    onImportGuestRideClick: () -> Unit,
    onDismissGuestImportSheet: () -> Unit,
    onChooseGuestFileClick: () -> Unit,
    onGuestRiderNameChange: (String) -> Unit,
    onConfirmGuestImport: () -> Unit,
    onGuestAttemptDeleteClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hoveredAttemptId by remember { mutableStateOf<Long?>(null) }
    // Hover (pointer devices only) previews live and wins while active; the persisted tap
    // selection is what touch users actually see, and what survives navigating away and back.
    val highlightedAttemptId = hoveredAttemptId ?: uiState.selectedAttemptId

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.segment?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onImportGuestRideClick) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Import a guest ride")
                    }
                    if (uiState.attempts.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = "Has attempts",
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

        val segment = uiState.segment
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (uiState.stravaNotConnected) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Connect Strava in Settings to see this segment's star status.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = onGoToSettingsClick) { Text("Go to Settings") }
                    }
                }
            }
            if (segment != null) {
                item {
                    val place = listOfNotNull(segment.city, segment.state).joinToString(", ")
                    if (place.isNotBlank()) {
                        Text(
                            text = place,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialThemeExtras.textTertiary,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                        )
                    }
                }
                item { StatsRow(segment = segment, attemptCount = uiState.attempts.size) }
                if (uiState.routePoints.size >= 2) {
                    item {
                        RoutePreviewCard(
                            routePoints = uiState.routePoints,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        )
                    }
                }
            }

            uiState.personalBest?.let { pr ->
                item { PersonalBestCard(pr = pr, deltaSeconds = uiState.personalBestDeltaSeconds) }
            }

            // Sticky: scrolls away with the map/stats above it, then pins at the top once reached,
            // so the chart stays visible while the rest of "All Attempts" scrolls underneath it.
            if (uiState.progressPoints.size >= 2) {
                stickyHeader {
                    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                        Text(
                            text = "Progress Over Time",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialThemeExtras.textTertiary,
                            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 10.dp),
                        )
                        ProgressChart(
                            points = uiState.progressPoints,
                            highlightedAttemptId = highlightedAttemptId,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp).padding(bottom = 12.dp),
                        )
                    }
                }
            }

            if (uiState.attempts.isEmpty() && uiState.excludedAttempts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No rides have matched this segment yet. Import a FIT or GPX ride that passes through it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                if (uiState.attempts.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 10.dp),
                        ) {
                            Text(
                                text = "All Attempts",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialThemeExtras.textTertiary,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = onToggleAttemptsOrder) {
                                Icon(
                                    imageVector = Icons.Filled.SwapVert,
                                    contentDescription = "Reverse order",
                                    tint = MaterialThemeExtras.textTertiary,
                                )
                            }
                        }
                    }
                    items(uiState.attempts, key = { it.id }) { attempt ->
                        ExcludableAttemptRow(
                            item = attempt,
                            isSelected = attempt.id == uiState.selectedAttemptId,
                            onClick = { attemptId ->
                                onAttemptSelected(attemptId)
                                onAttemptClick(attemptId)
                            },
                            onHoverChange = { isHovered ->
                                hoveredAttemptId = if (isHovered) attempt.id else hoveredAttemptId.takeUnless { it == attempt.id }
                            },
                            onExcluded = onAttemptExcluded,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }

                if (uiState.excludedAttempts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Excluded",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialThemeExtras.textTertiary,
                            modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 10.dp),
                        )
                    }
                    items(uiState.excludedAttempts, key = { it.id }) { attempt ->
                        IncludableAttemptRow(
                            item = attempt,
                            onIncluded = onAttemptIncluded,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            if (uiState.guestAttempts.isNotEmpty()) {
                item {
                    Text(
                        text = "Guest Rides",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialThemeExtras.textTertiary,
                        modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 10.dp),
                    )
                }
                items(uiState.guestAttempts, key = { it.id }) { guestAttempt ->
                    GuestAttemptRow(
                        item = guestAttempt,
                        onDelete = onGuestAttemptDeleteClick,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }

    uiState.guestImportSheet?.let { sheetState ->
        ModalBottomSheet(onDismissRequest = onDismissGuestImportSheet, sheetState = rememberModalBottomSheetState()) {
            GuestImportSheet(
                state = sheetState,
                onChooseFileClick = onChooseGuestFileClick,
                onRiderNameChange = onGuestRiderNameChange,
                onConfirmClick = onConfirmGuestImport,
            )
        }
    }

    uiState.starPrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = onDismissStarPrompt,
            title = { Text("Star this segment?") },
            text = {
                Text(
                    "\"${uiState.segment?.name}\" isn't starred on your Strava account. " +
                        "Starring it keeps it included in future syncs.",
                )
            },
            confirmButton = {
                TextButton(onClick = onStarSegmentClick, enabled = !prompt.isSaving) { Text("Star") }
            },
            dismissButton = {
                TextButton(onClick = onDismissStarPrompt, enabled = !prompt.isSaving) { Text("Not now") }
            },
        )
    }
}

@Composable
private fun StatsRow(segment: Segment, attemptCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatCard(label = "DISTANCE", value = "%.1f km".format(segment.distanceMeters / 1000.0))
        StatCard(label = "AVG GRADIENT", value = "%.1f%%".format(segment.averageGradePercent))
        StatCard(label = "ELEV. GAIN", value = "%.0f m".format(segment.elevationGainMeters))
        StatCard(label = "ATTEMPTS", value = attemptCount.toString())
    }
}

@Composable
private fun PersonalBestCard(pr: AttemptItem, deltaSeconds: Long?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(
                text = "PERSONAL BEST",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
            Text(text = pr.durationLabel, fontWeight = FontWeight.Bold, fontSize = 32.sp)
            if (deltaSeconds != null && deltaSeconds > 0) {
                Text(
                    text = "-${deltaSeconds}s",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialThemeExtras.faster,
                    modifier = Modifier.padding(start = 10.dp, bottom = 4.dp),
                )
            }
        }
        Text(
            text = "${pr.dateLabel} · ${pr.lapLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialThemeExtras.textTertiary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
