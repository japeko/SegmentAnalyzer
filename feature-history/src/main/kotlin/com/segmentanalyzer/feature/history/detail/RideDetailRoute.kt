package com.segmentanalyzer.feature.history.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RideDetailRoute(
    onBackClick: () -> Unit,
    onSegmentClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RideDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RideDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onSegmentClick = onSegmentClick,
        modifier = modifier,
    )
}
