package com.segmentanalyzer.feature.history.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.core.ui.ElevationSparkline
import com.segmentanalyzer.core.ui.SourceTag
import com.segmentanalyzer.core.ui.StatCard
import com.segmentanalyzer.core.ui.label
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.feature.history.detail.components.StravaSegmentEffortDetailPanel
import com.segmentanalyzer.feature.history.detail.components.StravaSegmentEffortRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    uiState: RideDetailUiState,
    onBackClick: () -> Unit,
    onFetchStravaSegmentsClick: () -> Unit,
    onStravaSegmentEffortClick: (Int, String) -> Unit,
    onEffortLongPress: (String) -> Unit,
    onEffortSelectionToggled: (String) -> Unit,
    onExitEffortSelectionMode: () -> Unit,
    onFetchSelectedEffortsClick: () -> Unit,
    onGoToSettingsClick: () -> Unit,
    onEditClick: () -> Unit,
    onDismissEdit: () -> Unit,
    onEditNameChange: (String) -> Unit,
    onEditTagChange: (String) -> Unit,
    onEditTagSuggestionClick: (String) -> Unit,
    onEditActivityTypeChange: (ActivityType) -> Unit,
    onSaveEditClick: () -> Unit,
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
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                    if (uiState.ride != null) {
                        IconButton(onClick = onEditClick) {
                            Icon(Icons.Filled.Edit, contentDescription = "Rename or tag this ride")
                        }
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
                        if (!ride.tag.isNullOrBlank()) {
                            Text(
                                text = ride.tag,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        SourceTag(source = ride.source)
                    }
                }
                item { StatsRow(ride = ride) }
                if (ride.elevationProfile.size >= 2) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                            ElevationSparkline(profile = ride.elevationProfile)
                        }
                    }
                }
            }

            val isEffortSelectionMode = uiState.selectedEffortIds.isNotEmpty()
            item {
                when {
                    isEffortSelectionMode -> EffortSelectionActionRow(
                        selectedCount = uiState.selectedEffortIds.size,
                        isFetching = uiState.isFetchingSelectedEfforts,
                        onFetchClick = onFetchSelectedEffortsClick,
                        onCancelClick = onExitEffortSelectionMode,
                    )
                    uiState.bulkFetchResult != null -> Text(
                        text = "Saved ${uiState.bulkFetchResult.newlyImportedCount} new segment(s)." +
                            if (uiState.bulkFetchResult.alreadyImportedCount > 0) {
                                " ${uiState.bulkFetchResult.alreadyImportedCount} already imported."
                            } else {
                                ""
                            },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialThemeExtras.textTertiary,
                        modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 10.dp),
                    )
                    else -> Text(
                        text = "Segments in this Ride",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialThemeExtras.textTertiary,
                        modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 10.dp),
                    )
                }
            }
            stravaEffortsSection(
                state = uiState.stravaSegmentEfforts,
                expandedDetail = uiState.expandedSegmentEffortDetail,
                isSelectionMode = isEffortSelectionMode,
                selectedEffortIds = uiState.selectedEffortIds,
                onFetchClick = onFetchStravaSegmentsClick,
                onEffortClick = onStravaSegmentEffortClick,
                onEffortLongClick = onEffortLongPress,
                onEffortSelectionToggled = onEffortSelectionToggled,
                onGoToSettingsClick = onGoToSettingsClick,
            )
        }
    }

    uiState.editDialog?.let { dialog ->
        EditRideDialog(
            dialog = dialog,
            onDismiss = onDismissEdit,
            onNameChange = onEditNameChange,
            onTagChange = onEditTagChange,
            onTagSuggestionClick = onEditTagSuggestionClick,
            onActivityTypeChange = onEditActivityTypeChange,
            onSaveClick = onSaveEditClick,
        )
    }
}

@Composable
private fun EditRideDialog(
    dialog: EditRideDialogState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onTagChange: (String) -> Unit,
    onTagSuggestionClick: (String) -> Unit,
    onActivityTypeChange: (ActivityType) -> Unit,
    onSaveClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Ride") },
        text = {
            Column {
                OutlinedTextField(
                    value = dialog.name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    items(ActivityType.entries) { type ->
                        FilterChip(
                            selected = dialog.activityType == type,
                            onClick = { onActivityTypeChange(type) },
                            label = { Text(type.label()) },
                        )
                    }
                }
                OutlinedTextField(
                    value = dialog.tag,
                    onValueChange = onTagChange,
                    label = { Text("Tag") },
                    placeholder = { Text("e.g. Race, Training") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                if (dialog.tagSuggestions.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        dialog.tagSuggestions.forEach { suggestion ->
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTagSuggestionClick(suggestion) }
                                    .padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSaveClick, enabled = dialog.name.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun LazyListScope.stravaEffortsSection(
    state: StravaEffortsUiState,
    expandedDetail: ExpandedSegmentEffortDetail?,
    isSelectionMode: Boolean,
    selectedEffortIds: Set<String>,
    onFetchClick: () -> Unit,
    onEffortClick: (Int, String) -> Unit,
    onEffortLongClick: (String) -> Unit,
    onEffortSelectionToggled: (String) -> Unit,
    onGoToSettingsClick: () -> Unit,
) {
    when (state) {
        StravaEffortsUiState.NotConnected -> item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Connect Strava in Settings to see this ride's segments.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onGoToSettingsClick) { Text("Go to Settings") }
            }
        }

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
            itemsIndexed(state.efforts) { index, effort ->
                Column {
                    StravaSegmentEffortRow(
                        item = effort,
                        isSelectionMode = isSelectionMode,
                        isSelected = effort.effortExternalId in selectedEffortIds,
                        onClick = {
                            if (isSelectionMode) {
                                onEffortSelectionToggled(effort.effortExternalId)
                            } else {
                                onEffortClick(index, effort.effortExternalId)
                            }
                        },
                        onLongClick = { onEffortLongClick(effort.effortExternalId) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    if (!isSelectionMode && expandedDetail?.effortIndex == index) {
                        StravaSegmentEffortDetailPanel(
                            state = expandedDetail.state,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
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
private fun EffortSelectionActionRow(
    selectedCount: Int,
    isFetching: Boolean,
    onFetchClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$selectedCount selected",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialThemeExtras.textTertiary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isFetching) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 16.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = onFetchClick) { Text("Get Strava Data") }
            }
            IconButton(onClick = onCancelClick, enabled = !isFetching) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
