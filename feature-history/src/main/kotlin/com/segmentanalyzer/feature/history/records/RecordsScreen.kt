package com.segmentanalyzer.feature.history.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.theme.SegmentAnalyzerTheme
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.SummaryPeriod
import com.segmentanalyzer.feature.history.history.components.SummaryPeriodRow
import com.segmentanalyzer.feature.history.records.components.RecordRow
import com.segmentanalyzer.feature.history.records.components.RecordsSelectionTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    uiState: RecordsUiState,
    onPeriodSelected: (SummaryPeriod) -> Unit,
    onRecordClick: (Long) -> Unit,
    onRecordLongPress: (Long) -> Unit,
    onRecordSelectionToggled: (Long) -> Unit,
    onExitSelectionMode: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelectionMode = uiState.selectedAttemptIds.isNotEmpty()

    Scaffold(
        modifier = modifier,
        topBar = {
            if (isSelectionMode) {
                RecordsSelectionTopBar(
                    selectedCount = uiState.selectedAttemptIds.size,
                    isExporting = uiState.isExporting,
                    onExitSelectionMode = onExitSelectionMode,
                    onExportClick = onExportClick,
                )
            } else {
                TopAppBar(title = { Text("Records") })
            }
        },
    ) { padding ->
        if (uiState.newPersonalBests.isEmpty() && uiState.otherRecords.isEmpty() && !uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No segment records yet. Import rides and match them against your " +
                        "starred segments to see records here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item {
                    SummaryPeriodRow(
                        selected = uiState.selectedPeriod,
                        onPeriodSelected = onPeriodSelected,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                uiState.exportSkippedMessage?.let { message ->
                    item {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                        )
                    }
                }
                item {
                    SectionHeader(title = "New PBs", modifier = Modifier.padding(top = 20.dp))
                }
                if (uiState.newPersonalBests.isEmpty()) {
                    item { EmptySectionText(text = "No new personal bests for this period.") }
                } else {
                    items(uiState.newPersonalBests, key = { it.attemptId }) { record ->
                        RecordRow(
                            item = record,
                            isNew = true,
                            isSelectionMode = isSelectionMode,
                            isSelected = record.attemptId in uiState.selectedAttemptIds,
                            onClick = {
                                if (isSelectionMode) onRecordSelectionToggled(record.attemptId) else onRecordClick(record.segmentId)
                            },
                            onLongClick = { onRecordLongPress(record.attemptId) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                        )
                    }
                }
                item {
                    SectionHeader(title = "Other Records", modifier = Modifier.padding(top = 16.dp))
                }
                if (uiState.otherRecords.isEmpty()) {
                    item { EmptySectionText(text = "No other records yet.") }
                } else {
                    items(uiState.otherRecords, key = { it.attemptId }) { record ->
                        RecordRow(
                            item = record,
                            isNew = false,
                            isSelectionMode = isSelectionMode,
                            isSelected = record.attemptId in uiState.selectedAttemptIds,
                            onClick = {
                                if (isSelectionMode) onRecordSelectionToggled(record.attemptId) else onRecordClick(record.segmentId)
                            },
                            onLongClick = { onRecordLongPress(record.attemptId) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 16.dp, bottom = 10.dp),
    )
}

@Composable
private fun EmptySectionText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

private val previewNewPBs = listOf(
    RecordListItem(
        attemptId = 1,
        segmentId = 1,
        segmentName = "Skyline Ridge Climb",
        distanceKm = 3.2,
        rideName = "Skyline Ridge Loop",
        rideSource = ActivitySource.GARMIN,
        dateLabel = "Aug 23, 2026",
        durationLabel = "12:04",
        avgSpeedKmh = 15.9,
    ),
)

private val previewOtherRecords = listOf(
    RecordListItem(
        attemptId = 2,
        segmentId = 2,
        segmentName = "Widow Creek Descent",
        distanceKm = 1.8,
        rideName = "Widow Creek Descent",
        rideSource = ActivitySource.STRAVA,
        dateLabel = "Jul 2, 2026",
        durationLabel = "3:41",
        avgSpeedKmh = 29.3,
    ),
)

private val previewState = RecordsUiState(
    isLoading = false,
    selectedPeriod = SummaryPeriod.THIS_MONTH,
    newPersonalBests = previewNewPBs,
    otherRecords = previewOtherRecords,
)

@Preview(name = "Records — Light", showBackground = true)
@Composable
private fun RecordsScreenLightPreview() {
    SegmentAnalyzerTheme(darkTheme = false) {
        RecordsScreen(
            uiState = previewState,
            onPeriodSelected = {},
            onRecordClick = {},
            onRecordLongPress = {},
            onRecordSelectionToggled = {},
            onExitSelectionMode = {},
            onExportClick = {},
        )
    }
}

@Preview(name = "Records — Dark", showBackground = true)
@Composable
private fun RecordsScreenDarkPreview() {
    SegmentAnalyzerTheme(darkTheme = true) {
        RecordsScreen(
            uiState = previewState,
            onPeriodSelected = {},
            onRecordClick = {},
            onRecordLongPress = {},
            onRecordSelectionToggled = {},
            onExitSelectionMode = {},
            onExportClick = {},
        )
    }
}
