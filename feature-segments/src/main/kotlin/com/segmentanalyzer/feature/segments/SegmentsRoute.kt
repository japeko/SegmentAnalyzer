package com.segmentanalyzer.feature.segments

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SegmentsRoute(
    onGoToSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SegmentsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SegmentsScreen(
        uiState = uiState,
        onSyncClick = viewModel::onSyncClick,
        onGoToSettingsClick = onGoToSettingsClick,
        modifier = modifier,
    )
}
