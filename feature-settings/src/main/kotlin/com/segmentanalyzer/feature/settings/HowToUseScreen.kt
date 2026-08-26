package com.segmentanalyzer.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.segmentanalyzer.core.theme.MaterialThemeExtras

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUseScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("How to Use") },
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
                .padding(bottom = 24.dp),
        ) {
            HowToSection(
                title = "Getting Started",
                steps = listOf(
                    "Connect Garmin Connect" to
                        "Settings → Connected Services → Connect. This is how rides get imported.",
                    "Connect Strava" to
                        "Required for segment data — without it, rides and segments show no segment info, just imported stats.",
                ),
            )

            HowToSection(
                title = "Rides",
                steps = listOf(
                    "Import rides" to
                        "Tap + on the Rides tab to sign in with Garmin Connect and pull in recent activities.",
                    "Open a ride" to
                        "Tap it to see stats and, fetched live from Strava, every segment it passed through — needs a Strava activity recorded within a few minutes of the ride's start time, not just Strava being connected.",
                    "Delete a ride" to
                        "Swipe it left, confirm, then tap Undo on the snackbar if you change your mind.",
                    "Bulk edit" to
                        "Long-press a ride to select several, then set a tag or activity type for all of them at once.",
                ),
            )

            HowToSection(
                title = "Segments",
                steps = listOf(
                    "Sync starred segments" to
                        "Segments tab → Sync starred segments. Pulls in every segment you've starred on Strava, route included, and matches it against your existing rides.",
                    "Segments also add themselves" to
                        "Opening one of a ride's segments (even unstarred) saves it locally too, so it's browsable here without a manual sync.",
                    "Segment Detail" to
                        "Tap a segment for your personal best, a progress-over-time chart, and every attempt.",
                    "PR / 2nd / 3rd" to
                        "The three fastest attempts (that you haven't excluded) are marked automatically.",
                    "Exclude an attempt" to
                        "Swipe it left to drop it from the chart and personal-best calculation. Swipe an excluded one back in to restore it.",
                ),
            )

            HowToSection(
                title = "Records",
                steps = listOf(
                    "New PBs & other records" to
                        "The Records tab lists every personal best and other segment record, filtered to the period you pick (Week/Month/Year/All Time).",
                ),
            )

            HowToSection(
                title = "Compare Rides",
                steps = listOf(
                    "Open a comparison" to
                        "From Segment Detail, tap any attempt to compare it against your personal best and the previous attempt.",
                    "Add or remove rides" to
                        "Use + Add to bring in another attempt, or dismiss a chip to drop it from the comparison.",
                    "Scrub the charts" to
                        "Drag a finger across the Slope, Speed, or Time Gap chart — the position stays in sync across all three plus the route map.",
                ),
            )

            HowToSection(
                title = "Appearance",
                steps = listOf(
                    "Themes" to
                        "Settings → Appearance → pick Light, Lavender, Dark, Dracula, or Trailhead.",
                ),
            )
        }
    }
}

@Composable
private fun HowToSection(title: String, steps: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialThemeExtras.textTertiary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        steps.forEachIndexed { index, (stepTitle, description) ->
            HowToStepRow(number = index + 1, title = stepTitle, description = description)
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun HowToStepRow(number: Int, title: String, description: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "$number",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
