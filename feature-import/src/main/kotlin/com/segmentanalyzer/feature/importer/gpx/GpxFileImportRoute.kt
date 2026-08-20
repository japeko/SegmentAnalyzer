package com.segmentanalyzer.feature.importer.gpx

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GpxFileImportRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GpxFileImportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // GetContent (ACTION_GET_CONTENT) rather than OpenDocument (ACTION_OPEN_DOCUMENT): the latter
    // opens the Storage Access Framework's DocumentsUI tree, whose "Downloads" grid view requires
    // a two-step tap (select, then a separate "Select" button) that reads as "can't pick a file"
    // — GetContent is a single-tap chooser and needs no persistable URI grant since the file is
    // read immediately below, not held onto.
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.onFileSelected(uri.toString())
    }

    GpxFileImportScreen(
        uiState = uiState,
        onChooseFileClick = { pickFile.launch("*/*") },
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
