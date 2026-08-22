package com.segmentanalyzer.feature.history.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RideHistoryRoute(
    onRideClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RideHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RideHistoryScreen(
        uiState = uiState,
        onFilterSelected = viewModel::onFilterSelected,
        onPeriodSelected = viewModel::onPeriodSelected,
        onRideClick = onRideClick,
        onSearchClick = onSearchClick,
        onImportClick = onImportClick,
        modifier = modifier,
    )
}
