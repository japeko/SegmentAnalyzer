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
    onNewPBsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RideHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RideHistoryScreen(
        uiState = uiState,
        onFilterSelected = viewModel::onFilterSelected,
        onPeriodSelected = viewModel::onPeriodSelected,
        onRideClick = onRideClick,
        onRideLongPress = viewModel::onRideLongPress,
        onRideSelectionToggled = viewModel::onRideSelectionToggled,
        onExitSelectionMode = viewModel::onExitSelectionMode,
        onSetTagClick = viewModel::onSetTagClick,
        onTagDialogValueChange = viewModel::onTagDialogValueChange,
        onTagSuggestionClick = viewModel::onTagSuggestionClick,
        onDismissTagDialog = viewModel::onDismissTagDialog,
        onConfirmSetTag = viewModel::onConfirmSetTag,
        onSetActivityTypeClick = viewModel::onSetActivityTypeClick,
        onActivityTypeDialogSelected = viewModel::onActivityTypeDialogSelected,
        onDismissActivityTypeDialog = viewModel::onDismissActivityTypeDialog,
        onConfirmSetActivityType = viewModel::onConfirmSetActivityType,
        onSearchClick = onSearchClick,
        onImportClick = onImportClick,
        onNewPBsClick = onNewPBsClick,
        onDeleteRideRequested = viewModel::onDeleteRideRequested,
        onDismissDeleteRide = viewModel::onDismissDeleteRide,
        onConfirmDeleteRide = viewModel::onConfirmDeleteRide,
        onUndoDeleteRideClick = viewModel::onUndoDeleteRideClick,
        onUndoDeleteRideSnackbarDismissed = viewModel::onUndoDeleteRideSnackbarDismissed,
        modifier = modifier,
    )
}
