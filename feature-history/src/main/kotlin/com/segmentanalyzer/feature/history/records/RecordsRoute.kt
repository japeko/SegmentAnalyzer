package com.segmentanalyzer.feature.history.records

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

@Composable
fun RecordsRoute(
    onSegmentClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.exportedFiles.collect { files -> context.shareFitFiles(files) }
    }

    RecordsScreen(
        uiState = uiState,
        onPeriodSelected = viewModel::onPeriodSelected,
        onRecordClick = onSegmentClick,
        onRecordLongPress = viewModel::onRecordLongPress,
        onRecordSelectionToggled = viewModel::onRecordSelectionToggled,
        onExitSelectionMode = viewModel::onExitSelectionMode,
        onExportClick = viewModel::onExportClick,
        modifier = modifier,
    )
}

/** Hands [files] off to the system share sheet as .fit attachments, via each file's FileProvider content Uri. */
private fun android.content.Context.shareFitFiles(files: List<File>) {
    if (files.isEmpty()) return
    val authority = "$packageName.fileprovider"
    val uris = ArrayList(files.map { file -> FileProvider.getUriForFile(this, authority, file) })

    val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
        type = "application/vnd.ant.fit"
        if (uris.size == 1) {
            putExtra(Intent.EXTRA_STREAM, uris.first())
        } else {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, "Export records"))
}
