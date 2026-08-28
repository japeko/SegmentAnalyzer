package com.segmentanalyzer.feature.history.records.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsSelectionTopBar(
    selectedCount: Int,
    isExporting: Boolean,
    onExitSelectionMode: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onExitSelectionMode, enabled = !isExporting) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
            }
        },
        actions = {
            if (isExporting) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp).size(20.dp))
            } else {
                TextButton(onClick = onExportClick) { Text("Export") }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(),
    )
}
