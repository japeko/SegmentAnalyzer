package com.segmentanalyzer.feature.history.history.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideSelectionTopBar(
    selectedCount: Int,
    onExitSelectionMode: () -> Unit,
    onSetTagClick: () -> Unit,
    onSetActivityTypeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onExitSelectionMode) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
            }
        },
        actions = {
            TextButton(onClick = onSetActivityTypeClick) {
                Text("Type")
            }
            TextButton(onClick = onSetTagClick) {
                Text("Tag")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(),
    )
}
