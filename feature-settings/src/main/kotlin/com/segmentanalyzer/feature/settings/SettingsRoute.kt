package com.segmentanalyzer.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(
    onConnectGarminClick: () -> Unit,
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
        modifier = modifier,
    )
}
