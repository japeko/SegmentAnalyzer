package com.segmentanalyzer.feature.auth.strava

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun StravaCallbackRoute(
    onConnected: () -> Unit,
    onBackToSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StravaCallbackViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is StravaCallbackUiState.Connected) onConnected()
    }

    StravaCallbackScreen(
        uiState = uiState,
        onBackToSettingsClick = onBackToSettingsClick,
        modifier = modifier,
    )
}
