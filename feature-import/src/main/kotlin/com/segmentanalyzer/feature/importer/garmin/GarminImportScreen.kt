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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.ui.label
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val DATE_LABEL_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarminImportScreen(
    uiState: GarminImportUiState,
    onDateFromSelected: (LocalDate?) -> Unit,
    onDateToSelected: (LocalDate?) -> Unit,
    onBrowseRidesClick: () -> Unit,
    onRideToggled: (String) -> Unit,
    onSelectAllToggled: () -> Unit,
    onNameFilterChange: (String) -> Unit,
    onImportSelectedClick: () -> Unit,
    onBackToIdleClick: () -> Unit,
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
                onNameFilterChange = onNameFilterChange,
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

                is GarminImportUiState.Idle -> {
                    Text(
                        text = "Browse your Garmin Connect rides and choose which ones to import.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Optionally narrow the search to a date range.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) {
                        DateField(
                            label = "From",
                            date = uiState.dateFrom,
                            onDateSelected = onDateFromSelected,
                            modifier = Modifier.weight(1f),
                        )
                        DateField(
                            label = "To",
                            date = uiState.dateTo,
                            onDateSelected = onDateToSelected,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    val rangeIsInverted = uiState.dateFrom != null && uiState.dateTo != null && uiState.dateTo.isBefore(uiState.dateFrom)
                    if (rangeIsInverted) {
                        Text(
                            text = "\"To\" can't be before \"From\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Button(
                        onClick = onBrowseRidesClick,
                        enabled = !rangeIsInverted,
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                    ) {
                        Text("Browse rides")
                    }
                }

                GarminImportUiState.FetchingRides -> {
                    CircularProgressIndicator()
                    Text(
                        text = "Fetching your rides…",
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
                    OutlinedButton(onClick = onBackToIdleClick, modifier = Modifier.padding(top = 16.dp)) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, date: LocalDate?, onDateSelected: (LocalDate?) -> Unit, modifier: Modifier = Modifier) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { showPicker = true }, modifier = modifier) {
        Text(text = "$label: ${date?.let { DATE_LABEL_FORMATTER.format(it) } ?: "Any"}", maxLines = 1)
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    onDateSelected(millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate() })
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun RidePicker(
    state: GarminImportUiState.SelectingRides,
    onRideToggled: (String) -> Unit,
    onSelectAllToggled: () -> Unit,
    onNameFilterChange: (String) -> Unit,
    onImportSelectedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.candidates.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "No Garmin Connect rides found for that range.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val visible = state.visibleCandidates
    val allVisibleSelected = visible.isNotEmpty() && visible.all { it.externalId in state.selectedExternalIds }
    Column(modifier = modifier.fillMaxSize()) {
        if (state.dateFrom != null || state.dateTo != null) {
            Text(
                text = "${state.dateFrom?.let { DATE_LABEL_FORMATTER.format(it) } ?: "Any"} " +
                    "– ${state.dateTo?.let { DATE_LABEL_FORMATTER.format(it) } ?: "Any"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        OutlinedTextField(
            value = state.nameFilter,
            onValueChange = onNameFilterChange,
            label = { Text("Filter by name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${visible.count { it.externalId in state.selectedExternalIds }} of ${visible.size} selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onSelectAllToggled, enabled = visible.isNotEmpty()) {
                Text(if (allVisibleSelected) "Deselect all" else "Select all")
            }
        }

        if (visible.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "No rides match \"${state.nameFilter}\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(visible, key = { it.externalId }) { candidate ->
                    GarminRideCandidateRow(
                        item = candidate,
                        isSelected = candidate.externalId in state.selectedExternalIds,
                        onToggle = { onRideToggled(candidate.externalId) },
                    )
                }
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
