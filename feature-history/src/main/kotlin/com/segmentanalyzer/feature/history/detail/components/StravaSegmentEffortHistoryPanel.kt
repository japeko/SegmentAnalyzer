package com.segmentanalyzer.feature.history.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.core.theme.NumericFontFamily
import com.segmentanalyzer.feature.history.detail.StravaEffortHistoryUiState
import com.segmentanalyzer.feature.history.detail.StravaSegmentEffortHistoryItem

/** Expanded under a [StravaSegmentEffortRow] — the athlete's own past efforts on that segment. */
@Composable
fun StravaSegmentEffortHistoryPanel(state: StravaEffortHistoryUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, bottom = 4.dp)) {
        when (state) {
            StravaEffortHistoryUiState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            }

            is StravaEffortHistoryUiState.Error -> Text(
                text = state.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp),
            )

            is StravaEffortHistoryUiState.Loaded -> if (state.entries.isEmpty()) {
                Text(
                    text = "No effort history found for this segment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            } else {
                state.entries.forEach { entry -> EffortHistoryRow(entry) }
            }
        }
    }
}

@Composable
private fun EffortHistoryRow(entry: StravaSegmentEffortHistoryItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = entry.dateLabel, fontSize = 12.sp, color = MaterialThemeExtras.textTertiary)
            entry.prRank?.let { rank ->
                Text(
                    text = if (rank == 1) "PR" else "PR #$rank",
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            entry.komRank?.let { rank ->
                Text(
                    text = "KOM #$rank",
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        Text(text = entry.elapsedTimeLabel, fontFamily = NumericFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
