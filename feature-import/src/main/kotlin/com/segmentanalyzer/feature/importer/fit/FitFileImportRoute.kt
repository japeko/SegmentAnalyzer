package com.segmentanalyzer.feature.importer.fit

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FitFileImportRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FitFileImportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            // Some document providers don't support persistable grants — fine, we only need to
            // read the file once, right now, for the import below.
        }
        viewModel.onFileSelected(uri.toString())
    }

    FitFileImportScreen(
        uiState = uiState,
        onChooseFileClick = { pickFile.launch(arrayOf("*/*")) },
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
