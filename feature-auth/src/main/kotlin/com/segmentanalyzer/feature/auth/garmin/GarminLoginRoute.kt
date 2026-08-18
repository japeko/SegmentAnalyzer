package com.segmentanalyzer.feature.auth.garmin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GarminLoginRoute(
    onConnected: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GarminLoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isConnected) {
        if (uiState.isConnected) onConnected()
    }

    GarminLoginScreen(
        uiState = uiState,
        onUsernameChanged = viewModel::onUsernameChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onMfaCodeChanged = viewModel::onMfaCodeChanged,
        onConnectClick = viewModel::onConnectClick,
        onSubmitMfaCodeClick = viewModel::onSubmitMfaCodeClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
