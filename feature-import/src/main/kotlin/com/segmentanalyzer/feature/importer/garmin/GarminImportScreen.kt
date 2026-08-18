package com.segmentanalyzer.feature.importer.garmin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarminImportScreen(
    uiState: GarminImportUiState,
    onImportClick: () -> Unit,
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
                        text = "Import your most recent rides from Garmin Connect.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onImportClick, modifier = Modifier.padding(top = 16.dp).fillMaxWidth()) {
                        Text("Import recent rides")
                    }
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
                    val alreadyImported = uiState.fetchedCount - uiState.importedCount
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
                    OutlinedButton(onClick = onImportClick, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Import again")
                    }
                }

                is GarminImportUiState.Error -> {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = onImportClick, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
