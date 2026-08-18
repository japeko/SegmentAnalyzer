package com.segmentanalyzer.feature.auth.strava

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StravaCallbackScreen(
    uiState: StravaCallbackUiState,
    onBackToSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (uiState) {
            is StravaCallbackUiState.Connecting -> {
                CircularProgressIndicator()
                Text(
                    text = "Connecting to Strava…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            is StravaCallbackUiState.Connected -> {
                Text(text = "Connected!", style = MaterialTheme.typography.titleMedium)
            }
            is StravaCallbackUiState.Error -> {
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = onBackToSettingsClick, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Back to Settings")
                }
            }
        }
    }
}
