package com.segmentanalyzer.feature.importer.gpx

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
fun GpxFileImportScreen(
    uiState: GpxImportUiState,
    onChooseFileClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Import GPX File") },
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
                GpxImportUiState.Idle -> {
                    Text(
                        text = "Pick a .gpx file from this device to import it as a ride.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onChooseFileClick,
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                    ) {
                        Text("Choose GPX file")
                    }
                }

                GpxImportUiState.Importing -> {
                    CircularProgressIndicator()
                    Text(
                        text = "Reading file…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                is GpxImportUiState.Result -> {
                    Text(text = "Imported \"${uiState.rideName}\".", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "%.1f km · %.0f m gain".format(uiState.distanceKm, uiState.elevationGainMeters),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    OutlinedButton(onClick = onChooseFileClick, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Import another")
                    }
                }

                is GpxImportUiState.Error -> {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = onChooseFileClick, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Try another file")
                    }
                }
            }
        }
    }
}
