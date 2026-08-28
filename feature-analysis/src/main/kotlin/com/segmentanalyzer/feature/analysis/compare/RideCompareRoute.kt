package com.segmentanalyzer.feature.analysis.compare

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RideCompareRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RideCompareViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RideCompareScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onAddClick = viewModel::onAddClick,
        onRemoveClick = viewModel::onRemoveAttempt,
        onSetReferenceClick = viewModel::onSetReferenceClick,
        onDismissAddSheet = viewModel::onDismissAddSheet,
        onAddableAttemptSelected = viewModel::onAddableAttemptSelected,
        onConfirmAdd = viewModel::onConfirmAdd,
        onGenerateInsightClick = viewModel::onGenerateInsightClick,
        modifier = modifier,
    )
}
