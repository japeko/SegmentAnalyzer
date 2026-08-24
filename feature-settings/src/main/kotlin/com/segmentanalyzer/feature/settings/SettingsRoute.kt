package com.segmentanalyzer.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(
    onConnectGarminClick: () -> Unit,
    onConnectStravaClick: (authorizationUrl: String) -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        onConnectGarminClick = onConnectGarminClick,
        onDisconnectGarminClick = viewModel::onDisconnectGarminClick,
        onConfirmDisconnectGarmin = viewModel::onConfirmDisconnectGarmin,
        onDismissDisconnectGarmin = viewModel::onDismissDisconnectGarmin,
        onConnectStravaClick = onConnectStravaClick,
        onDisconnectStravaClick = viewModel::onDisconnectStravaClick,
        onConfirmDisconnectStrava = viewModel::onConfirmDisconnectStrava,
        onDismissDisconnectStrava = viewModel::onDismissDisconnectStrava,
        stravaAuthorizationUrl = viewModel.stravaAuthorizationUrl,
        onThemeSelected = viewModel::onThemeSelected,
        onAboutClick = onAboutClick,
        modifier = modifier,
    )
}
