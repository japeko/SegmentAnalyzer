package com.segmentanalyzer.feature.importer.garmin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GarminImportRoute(
    onGoToSettingsClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GarminImportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GarminImportScreen(
        uiState = uiState,
        onImportClick = viewModel::onImportClick,
        onGoToSettingsClick = onGoToSettingsClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
