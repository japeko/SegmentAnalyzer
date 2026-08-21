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
import com.segmentanalyzer.feature.history.detail.StravaEffortDetailUiState
import com.segmentanalyzer.feature.history.detail.StravaSegmentEffortDetailItem

/** Expanded under a [StravaSegmentEffortRow] — pace/power/HR/cadence detail for that effort. */
@Composable
fun StravaSegmentEffortDetailPanel(state: StravaEffortDetailUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, bottom = 4.dp)) {
        when (state) {
            StravaEffortDetailUiState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            }

            is StravaEffortDetailUiState.Error -> Text(
                text = state.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp),
            )

            is StravaEffortDetailUiState.Loaded -> DetailStats(state.detail)
        }
    }
}

@Composable
private fun DetailStats(detail: StravaSegmentEffortDetailItem, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 6.dp)) {
        StatRow("Avg speed", detail.avgSpeedLabel)
        StatRow("Max speed", detail.maxSpeedLabel)
        StatRow("Elevation gain", detail.elevationGainLabel)
        detail.avgWattsLabel?.let { StatRow("Avg power", it) }
        detail.avgHeartRateLabel?.let { StatRow("Avg heart rate", it) }
        detail.avgCadenceLabel?.let { StatRow("Avg cadence", it) }
    }
}

@Composable
private fun StatRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialThemeExtras.textTertiary)
        Text(text = value, fontFamily = NumericFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
