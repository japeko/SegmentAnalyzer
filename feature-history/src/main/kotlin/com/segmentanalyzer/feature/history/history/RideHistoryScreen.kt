package com.segmentanalyzer.feature.history.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.theme.SegmentAnalyzerTheme
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.SummaryPeriod
import com.segmentanalyzer.domain.usecase.RideSummary
import com.segmentanalyzer.feature.history.history.components.ActivityTypeFilterRow
import com.segmentanalyzer.feature.history.history.components.BulkActivityTypeDialog
import com.segmentanalyzer.feature.history.history.components.BulkTagDialog
import com.segmentanalyzer.feature.history.history.components.ImportFab
import com.segmentanalyzer.feature.history.history.components.QuickStatsRow
import com.segmentanalyzer.feature.history.history.components.RideCard
import com.segmentanalyzer.feature.history.history.components.RideHistoryTopBar
import com.segmentanalyzer.feature.history.history.components.RideSelectionTopBar
import com.segmentanalyzer.feature.history.history.components.SummaryPeriodRow

@Composable
fun RideHistoryScreen(
    uiState: RideHistoryUiState,
    onFilterSelected: (ActivityType?) -> Unit,
    onPeriodSelected: (SummaryPeriod) -> Unit,
    onRideClick: (Long) -> Unit,
    onRideLongPress: (Long) -> Unit,
    onRideSelectionToggled: (Long) -> Unit,
    onExitSelectionMode: () -> Unit,
    onSetTagClick: () -> Unit,
    onTagDialogValueChange: (String) -> Unit,
    onTagSuggestionClick: (String) -> Unit,
    onDismissTagDialog: () -> Unit,
    onConfirmSetTag: () -> Unit,
    onSetActivityTypeClick: () -> Unit,
    onActivityTypeDialogSelected: (ActivityType) -> Unit,
    onDismissActivityTypeDialog: () -> Unit,
    onConfirmSetActivityType: () -> Unit,
    onSearchClick: () -> Unit,
    onImportClick: () -> Unit,
    onNewPBsClick: () -> Unit,
    onDeleteRideRequested: (Long) -> Unit,
    onDismissDeleteRide: () -> Unit,
    onConfirmDeleteRide: () -> Unit,
    onUndoDeleteRideClick: () -> Unit,
    onUndoDeleteRideSnackbarDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelectionMode = uiState.selectedRideIds.isNotEmpty()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.undoDeleteRide) {
        val undo = uiState.undoDeleteRide ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Deleted \"${undo.rideName}\"",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) onUndoDeleteRideClick() else onUndoDeleteRideSnackbarDismissed()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                RideSelectionTopBar(
                    selectedCount = uiState.selectedRideIds.size,
                    onExitSelectionMode = onExitSelectionMode,
                    onSetTagClick = onSetTagClick,
                    onSetActivityTypeClick = onSetActivityTypeClick,
                )
            } else {
                RideHistoryTopBar(onSearchClick = onSearchClick)
            }
        },
        floatingActionButton = { if (!isSelectionMode) ImportFab(onClick = onImportClick) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                SummaryPeriodRow(
                    selected = uiState.summaryPeriod,
                    onPeriodSelected = onPeriodSelected,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item {
                QuickStatsRow(
                    summary = uiState.summary,
                    period = uiState.summaryPeriod,
                    modifier = Modifier.padding(top = 10.dp),
                    onNewPBsClick = onNewPBsClick
                )
            }
            item {
                ActivityTypeFilterRow(
                    selected = uiState.selectedFilter,
                    onFilterSelected = onFilterSelected,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            item {
                Text(
                    text = "Recent Rides",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp),
                )
            }
            items(uiState.rides, key = { it.id }) { ride ->
                RideCard(
                    item = ride,
                    isSelectionMode = isSelectionMode,
                    isSelected = ride.id in uiState.selectedRideIds,
                    onClick = { id -> if (isSelectionMode) onRideSelectionToggled(id) else onRideClick(id) },
                    onLongClick = onRideLongPress,
                    onDeleteRequested = onDeleteRideRequested,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                )
            }
        }
    }

    uiState.tagDialog?.let { dialog ->
        BulkTagDialog(
            dialog = dialog,
            onDismiss = onDismissTagDialog,
            onTagChange = onTagDialogValueChange,
            onTagSuggestionClick = onTagSuggestionClick,
            onSaveClick = onConfirmSetTag,
        )
    }

    uiState.activityTypeDialog?.let { dialog ->
        BulkActivityTypeDialog(
            dialog = dialog,
            onTypeSelected = onActivityTypeDialogSelected,
            onDismiss = onDismissActivityTypeDialog,
            onSaveClick = onConfirmSetActivityType,
        )
    }

    uiState.pendingDeleteRide?.let { ride ->
        AlertDialog(
            onDismissRequest = onDismissDeleteRide,
            title = { Text("Delete this ride?") },
            text = { Text("\"${ride.name}\" and its segment data will be removed. You can undo this right after.") },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteRide) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteRide) { Text("Cancel") }
            },
        )
    }
}

private val previewRides = listOf(
    RideListItem(
        id = 1,
        name = "Skyline Ridge Loop",
        activityType = ActivityType.MTB,
        source = ActivitySource.GARMIN,
        dateLabel = "Aug 16, 2026",
        distanceKm = 14.2,
        durationLabel = "45:12",
        elevationGainMeters = 336.0,
        avgSpeedKmh = 18.8,
        isPersonalBest = false,
        elevationProfile = listOf(0.1f, 0.4f, 0.8f, 1f, 0.5f, 0.2f),
        tag = null,
    ),
    RideListItem(
        id = 2,
        name = "Widow Creek Descent",
        activityType = ActivityType.MTB,
        source = ActivitySource.GARMIN,
        dateLabel = "Aug 14, 2026",
        distanceKm = 8.7,
        durationLabel = "22:04",
        elevationGainMeters = 96.0,
        avgSpeedKmh = 23.6,
        isPersonalBest = true,
        elevationProfile = listOf(0.9f, 0.6f, 0.4f, 0.2f, 0.1f, 0.05f),
        tag = "Race",
    ),
)

private val previewState = RideHistoryUiState(
    isLoading = false,
    summary = RideSummary(
        totalDistanceKm = 140.4,
        rideCount = 5,
        elevationGainMeters = 1644.0,
        newPersonalBestCount = 2,
    ),
    summaryPeriod = SummaryPeriod.THIS_MONTH,
    selectedFilter = null,
    rides = previewRides,
)

@Preview(name = "Ride History — Light", showBackground = true)
@Composable
private fun RideHistoryScreenLightPreview() {
    SegmentAnalyzerTheme(darkTheme = false) {
        RideHistoryScreen(
            uiState = previewState,
            onFilterSelected = {},
            onPeriodSelected = {},
            onRideClick = {},
            onRideLongPress = {},
            onRideSelectionToggled = {},
            onExitSelectionMode = {},
            onSetTagClick = {},
            onTagDialogValueChange = {},
            onTagSuggestionClick = {},
            onDismissTagDialog = {},
            onConfirmSetTag = {},
            onSetActivityTypeClick = {},
            onActivityTypeDialogSelected = {},
            onDismissActivityTypeDialog = {},
            onConfirmSetActivityType = {},
            onSearchClick = {},
            onImportClick  = {},
            onNewPBsClick = {},
            onDeleteRideRequested = {},
            onDismissDeleteRide = {},
            onConfirmDeleteRide = {},
            onUndoDeleteRideClick = {},
            onUndoDeleteRideSnackbarDismissed = {},
        )
    }
}

@Preview(name = "Ride History — Dark", showBackground = true)
@Composable
private fun RideHistoryScreenDarkPreview() {
    SegmentAnalyzerTheme(darkTheme = true) {
        RideHistoryScreen(
            uiState = previewState,
            onFilterSelected = {},
            onPeriodSelected = {},
            onRideClick = {},
            onRideLongPress = {},
            onRideSelectionToggled = {},
            onExitSelectionMode = {},
            onSetTagClick = {},
            onTagDialogValueChange = {},
            onTagSuggestionClick = {},
            onDismissTagDialog = {},
            onConfirmSetTag = {},
            onSetActivityTypeClick = {},
            onActivityTypeDialogSelected = {},
            onDismissActivityTypeDialog = {},
            onConfirmSetActivityType = {},
            onSearchClick = {},
            onImportClick = {},
            onNewPBsClick = {},
            onDeleteRideRequested = {},
            onDismissDeleteRide = {},
            onConfirmDeleteRide = {},
            onUndoDeleteRideClick = {},
            onUndoDeleteRideSnackbarDismissed = {},
        )
    }
}
