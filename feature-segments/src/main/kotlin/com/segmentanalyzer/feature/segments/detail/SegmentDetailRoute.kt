package com.segmentanalyzer.feature.segments.detail

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SegmentDetailRoute(
    onBackClick: () -> Unit,
    onAttemptClick: (segmentId: Long, attemptId: Long) -> Unit,
    onGoToSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SegmentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // GetContent, not OpenDocument — same reasoning as FitFileImportRoute: a single-tap chooser,
    // and the file is read immediately below rather than held onto, so no persistable grant needed.
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val fileName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        viewModel.onGuestFileSelected(uri.toString(), fileName)
    }

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
        onGoToSettingsClick = onGoToSettingsClick,
        onImportGuestRideClick = viewModel::onImportGuestRideClick,
        onDismissGuestImportSheet = viewModel::onDismissGuestImportSheet,
        onChooseGuestFileClick = { pickFile.launch("*/*") },
        onGuestRiderNameChange = viewModel::onGuestRiderNameChange,
        onConfirmGuestImport = viewModel::onConfirmGuestImport,
        onGuestAttemptDeleteClick = viewModel::onGuestAttemptDeleteClick,
        modifier = modifier,
    )
}
