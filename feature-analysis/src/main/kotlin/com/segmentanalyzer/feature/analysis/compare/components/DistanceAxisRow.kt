package com.segmentanalyzer.feature.analysis.compare.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A shared km-mark header for the stacked Elevation/Speed/Time Gap charts below it, all of which
 * plot the same 0..[segmentDistanceMeters] x-axis — one row of labels instead of repeating an
 * axis under every chart keeps the whole stack short enough to read on a phone without scrolling.
 */
@Composable
fun DistanceAxisRow(segmentDistanceMeters: Double, tickCount: Int = 5, modifier: Modifier = Modifier) {
    if (segmentDistanceMeters <= 0.0) return
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        for (index in 0 until tickCount) {
            val distanceKm = (segmentDistanceMeters / 1000.0) * index / (tickCount - 1)
            Text(text = "%.1f km".format(distanceKm), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
