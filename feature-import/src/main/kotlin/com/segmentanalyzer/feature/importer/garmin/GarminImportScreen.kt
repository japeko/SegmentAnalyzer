package com.segmentanalyzer.feature.importer.garmin

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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.domain.model.ActivityType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarminImportScreen(
    uiState: GarminImportUiState,
    onBrowseRidesClick: () -> Unit,
    onRideToggled: (String) -> Unit,
    onSelectAllToggled: () -> Unit,
    onImportSelectedClick: () -> Unit,
    onGoToSettingsClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Import from Garmin") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (uiState is GarminImportUiState.SelectingRides) {
            RidePicker(
                state = uiState,
                onRideToggled = onRideToggled,
                onSelectAllToggled = onSelectAllToggled,
                onImportSelectedClick = onImportSelectedClick,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (uiState) {
                GarminImportUiState.NotConnected -> {
                    Text(
                        text = "Connect your Garmin Connect account in Settings to import rides.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onGoToSettingsClick, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Go to Settings")
                    }
                }

                GarminImportUiState.Idle -> {
                    Text(
                        text = "Browse your recent Garmin Connect rides and choose which ones to import.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onBrowseRidesClick, modifier = Modifier.padding(top = 16.dp).fillMaxWidth()) {
                        Text("Browse rides")
                    }
                }

                GarminImportUiState.FetchingRides -> {
                    CircularProgressIndicator()
                    Text(
                        text = "Fetching your recent rides…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                GarminImportUiState.Importing -> {
                    CircularProgressIndicator()
                    Text(
                        text = "Importing rides…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                is GarminImportUiState.Result -> {
                    val alreadyImported = uiState.selectedCount - uiState.importedCount
                    Text(
                        text = "Imported ${uiState.importedCount} new ride(s).",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (alreadyImported > 0) {
                        Text(
                            text = "$alreadyImported already up to date.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    OutlinedButton(onClick = onBrowseRidesClick, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Browse more rides")
                    }
                }

                is GarminImportUiState.Error -> {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = onBrowseRidesClick, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Retry")
                    }
                }

                is GarminImportUiState.SelectingRides -> Unit // handled above, before this Column
            }
        }
    }
}

@Composable
private fun RidePicker(
    state: GarminImportUiState.SelectingRides,
    onRideToggled: (String) -> Unit,
    onSelectAllToggled: () -> Unit,
    onImportSelectedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.candidates.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "No recent rides found on Garmin Connect.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val allSelected = state.selectedExternalIds.size == state.candidates.size
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${state.selectedExternalIds.size} of ${state.candidates.size} selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onSelectAllToggled) {
                Text(if (allSelected) "Deselect all" else "Select all")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            items(state.candidates, key = { it.externalId }) { candidate ->
                GarminRideCandidateRow(
                    item = candidate,
                    isSelected = candidate.externalId in state.selectedExternalIds,
                    onToggle = { onRideToggled(candidate.externalId) },
                )
            }
        }

        Button(
            onClick = onImportSelectedClick,
            enabled = state.selectedExternalIds.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text("Import ${state.selectedExternalIds.size} ride(s)")
        }
    }
}

@Composable
private fun GarminRideCandidateRow(
    item: GarminRideCandidateItem,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = isSelected, onCheckedChange = null)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${item.activityType.label()} · ${item.dateLabel} · %.1f km · ${item.durationLabel}".format(item.distanceKm),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun ActivityType.label(): String = when (this) {
    ActivityType.MTB -> "MTB"
    ActivityType.EMTB -> "E-MTB"
    ActivityType.GRAVEL -> "Gravel"
    ActivityType.EGRAVEL -> "E-Gravel"
    ActivityType.ROAD -> "Road"
    ActivityType.EROAD -> "E-Road"
    ActivityType.OTHER -> "Other"
}
