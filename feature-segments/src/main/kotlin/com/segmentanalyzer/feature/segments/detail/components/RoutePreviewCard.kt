package com.segmentanalyzer.feature.segments.detail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A static start→end route preview. The app only stores a segment's start/end coordinates (not
 * a full polyline), so this draws a simple diagonal line rather than a real map — no MapLibre
 * dependency for V1, matching [com.segmentanalyzer.core.ui.ElevationSparkline]'s hand-rolled
 * Canvas approach.
 */
@Composable
fun RoutePreviewCard(modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val startColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        val padding = 24.dp.toPx()
        val start = Offset(padding, padding)
        val end = Offset(size.width - padding, size.height - padding)

        drawLine(color = lineColor, start = start, end = end, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(color = startColor, radius = 6.dp.toPx(), center = start)
        drawCircle(color = lineColor, radius = 6.dp.toPx(), center = end)
    }
}
