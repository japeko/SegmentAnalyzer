package com.segmentanalyzer.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.ui.FeatureRow

/**
 * Roadmap items shown in Settings so riders know what's planned without it being mistaken for
 * what the app can already do today.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingFeaturesScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Coming Soon") },
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
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Planned, not yet built — here's what's on the roadmap.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            FeatureRow(
                icon = Icons.Filled.Watch,
                accent = MaterialTheme.colorScheme.primary,
                title = "Polar Connect",
                description = "Import rides recorded on Polar devices, alongside Garmin.",
            )
            FeatureRow(
                icon = Icons.Filled.Leaderboard,
                accent = MaterialTheme.colorScheme.primary,
                title = "Leaderboards",
                description = "See how your times on a segment compare to other Segment Analyzer users who've shared their attempts.",
            )
        }
    }
}
