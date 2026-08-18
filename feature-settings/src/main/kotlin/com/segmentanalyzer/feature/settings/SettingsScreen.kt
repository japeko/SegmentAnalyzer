package com.segmentanalyzer.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.ui.SourceTag
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.GarminConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onConnectGarminClick: () -> Unit,
    onDisconnectGarminClick: () -> Unit,
    onConfirmDisconnectGarmin: () -> Unit,
    onDismissDisconnectGarmin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Connected Services",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            GarminConnectionCard(
                connectionState = uiState.garminConnectionState,
                onConnectClick = onConnectGarminClick,
                onDisconnectClick = onDisconnectGarminClick,
            )
        }
    }

    if (uiState.showDisconnectGarminConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissDisconnectGarmin,
            title = { Text("Disconnect Garmin Connect?") },
            text = { Text("Segment Analyzer will no longer be able to import rides from Garmin Connect until you reconnect.") },
            confirmButton = {
                TextButton(onClick = onConfirmDisconnectGarmin) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDisconnectGarmin) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun GarminConnectionCard(
    connectionState: GarminConnectionState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = connectionState is GarminConnectionState.Connected

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SourceTag(source = ActivitySource.GARMIN)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusDot(connected = isConnected)
                    Text(
                        text = when (connectionState) {
                            is GarminConnectionState.Connected -> "Connected as ${connectionState.username}"
                            GarminConnectionState.Disconnected -> "Not connected"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            OutlinedButton(onClick = if (isConnected) onDisconnectClick else onConnectClick) {
                Text(if (isConnected) "Disconnect" else "Connect", maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun StatusDot(connected: Boolean, modifier: Modifier = Modifier) {
    val color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(modifier = modifier.size(8.dp).background(color, CircleShape))
}
