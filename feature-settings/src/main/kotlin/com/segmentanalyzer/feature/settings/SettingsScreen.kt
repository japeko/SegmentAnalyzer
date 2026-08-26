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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.core.ui.SourceTag
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.GarminConnectionState
import com.segmentanalyzer.domain.model.StravaConnectionState
import com.segmentanalyzer.domain.model.ThemePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onConnectGarminClick: () -> Unit,
    onDisconnectGarminClick: () -> Unit,
    onConfirmDisconnectGarmin: () -> Unit,
    onDismissDisconnectGarmin: () -> Unit,
    onConnectStravaClick: (authorizationUrl: String) -> Unit,
    onDisconnectStravaClick: () -> Unit,
    onConfirmDisconnectStrava: () -> Unit,
    onDismissDisconnectStrava: () -> Unit,
    stravaAuthorizationUrl: String,
    onThemeSelected: (ThemePreference) -> Unit,
    onAboutClick: () -> Unit,
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
            ConnectionCard(
                source = ActivitySource.GARMIN,
                isConnected = uiState.garminConnectionState is GarminConnectionState.Connected,
                statusText = when (val state = uiState.garminConnectionState) {
                    is GarminConnectionState.Connected ->
                        state.username.takeIf { it.isNotBlank() }?.let { "Connected as $it" } ?: "Connected"
                    GarminConnectionState.Disconnected -> "Not connected"
                },
                onConnectClick = onConnectGarminClick,
                onDisconnectClick = onDisconnectGarminClick,
            )
            ConnectionCard(
                source = ActivitySource.STRAVA,
                isConnected = uiState.stravaConnectionState is StravaConnectionState.Connected,
                statusText = when (val state = uiState.stravaConnectionState) {
                    is StravaConnectionState.Connected -> "Connected as ${state.athleteName}"
                    StravaConnectionState.Disconnected -> "Not connected"
                },
                onConnectClick = { onConnectStravaClick(stravaAuthorizationUrl) },
                onDisconnectClick = onDisconnectStravaClick,
            )

            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
            ThemeSelectorCard(selected = uiState.themePreference, onThemeSelected = onThemeSelected)

            Text(
                text = "App",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
            AboutRow(onClick = onAboutClick)
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

    if (uiState.showDisconnectStravaConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissDisconnectStrava,
            title = { Text("Disconnect Strava?") },
            text = { Text("Segment Analyzer will no longer be able to sync segments from Strava until you reconnect.") },
            confirmButton = {
                TextButton(onClick = onConfirmDisconnectStrava) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDisconnectStrava) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ConnectionCard(
    source: ActivitySource,
    isConnected: Boolean,
    statusText: String,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                SourceTag(source = source)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusDot(connected = isConnected)
                    Text(
                        text = statusText,
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
private fun ThemeSelectorCard(
    selected: ThemePreference,
    onThemeSelected: (ThemePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemePreference.entries.forEachIndexed { index, preference ->
                    SegmentedButton(
                        selected = preference == selected,
                        onClick = { onThemeSelected(preference) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemePreference.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            activeBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                        icon = {},
                        label = {
                            Icon(
                                imageVector = when (preference) {
                                    ThemePreference.LIGHT -> Icons.Filled.LightMode
                                    ThemePreference.LAVENDER -> Icons.Filled.Spa
                                    ThemePreference.DARK -> Icons.Filled.DarkMode
                                    ThemePreference.DRACULA -> Icons.Filled.Bloodtype
                                    ThemePreference.TRAILHEAD -> Icons.Filled.Timer
                                },
                                contentDescription = preference.label(),
                            )
                        },
                    )
                }
            }
            Text(
                text = selected.label(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

private fun ThemePreference.label(): String = when (this) {
    ThemePreference.LIGHT -> "Light"
    ThemePreference.LAVENDER -> "Lavender"
    ThemePreference.DARK -> "Dark"
    ThemePreference.DRACULA -> "Dracula"
    ThemePreference.TRAILHEAD -> "Trailhead"
}

@Composable
private fun AboutRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialThemeExtras.textTertiary,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(text = "About Segment Analyzer", style = MaterialTheme.typography.bodyMedium)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialThemeExtras.textTertiary,
            )
        }
    }
}

@Composable
private fun StatusDot(connected: Boolean, modifier: Modifier = Modifier) {
    val color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(modifier = modifier.size(8.dp).background(color, CircleShape))
}
