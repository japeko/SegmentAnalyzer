package com.segmentanalyzer.feature.history.records

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RecordsRoute(
    onSegmentClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RecordsScreen(
        uiState = uiState,
        onPeriodSelected = viewModel::onPeriodSelected,
        onRecordClick = onSegmentClick,
        modifier = modifier,
    )
}
