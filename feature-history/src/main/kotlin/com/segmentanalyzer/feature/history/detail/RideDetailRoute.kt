package com.segmentanalyzer.feature.history.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RideDetailRoute(
    onBackClick: () -> Unit,
    onGoToSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RideDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RideDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onFetchStravaSegmentsClick = viewModel::onFetchStravaSegmentsClick,
        onStravaSegmentEffortClick = viewModel::onStravaSegmentEffortClick,
        onEffortLongPress = viewModel::onEffortLongPress,
        onEffortSelectionToggled = viewModel::onEffortSelectionToggled,
        onExitEffortSelectionMode = viewModel::onExitEffortSelectionMode,
        onFetchSelectedEffortsClick = viewModel::onFetchSelectedEffortsClick,
        onGoToSettingsClick = onGoToSettingsClick,
        onEditClick = viewModel::onEditClick,
        onDismissEdit = viewModel::onDismissEdit,
        onEditNameChange = viewModel::onEditNameChange,
        onEditTagChange = viewModel::onEditTagChange,
        onEditTagSuggestionClick = viewModel::onEditTagSuggestionClick,
        onEditActivityTypeChange = viewModel::onEditActivityTypeChange,
        onSaveEditClick = viewModel::onSaveEditClick,
        modifier = modifier,
    )
}
