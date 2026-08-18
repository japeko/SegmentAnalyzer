package com.segmentanalyzer.feature.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Where to import rides from. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSourceScreen(
    onGarminClick: () -> Unit,
    onFitFileClick: () -> Unit,
    onGpxFileClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Import Rides") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onGarminClick, modifier = Modifier.fillMaxWidth()) {
                Text("Import from Garmin Connect")
            }
            OutlinedButton(onClick = onFitFileClick, modifier = Modifier.fillMaxWidth()) {
                Text("Import a FIT file")
            }
            OutlinedButton(onClick = onGpxFileClick, modifier = Modifier.fillMaxWidth()) {
                Text("Import a GPX file")
            }
        }
    }
}
