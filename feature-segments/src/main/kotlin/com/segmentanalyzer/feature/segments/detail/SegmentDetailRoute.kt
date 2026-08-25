package com.segmentanalyzer.feature.segments.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SegmentDetailRoute(
    onBackClick: () -> Unit,
    onAttemptClick: (segmentId: Long, attemptId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SegmentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SegmentDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onAttemptClick = { attemptId -> uiState.segment?.let { onAttemptClick(it.id, attemptId) } },
        onAttemptSelected = viewModel::onAttemptSelected,
        onAttemptExcluded = viewModel::onAttemptExcluded,
        onAttemptIncluded = viewModel::onAttemptIncluded,
        onToggleAttemptsOrder = viewModel::onToggleAttemptsOrder,
        onStarSegmentClick = viewModel::onStarSegmentClick,
        onDismissStarPrompt = viewModel::onDismissStarPrompt,
        modifier = modifier,
    )
}
