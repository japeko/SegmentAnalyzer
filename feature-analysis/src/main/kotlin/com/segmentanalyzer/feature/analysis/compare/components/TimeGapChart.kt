package com.segmentanalyzer.feature.analysis.compare.components

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.feature.analysis.compare.TimeGapSeriesUi
import kotlin.math.abs

/** One line per non-Current attempt: seconds ahead (negative, below zero-line) or behind (positive) Current, by distance. */
@Composable
fun TimeGapChart(series: List<TimeGapSeriesUi>, modifier: Modifier = Modifier) {
    val zeroLineColor = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val extras = MaterialThemeExtras
    val seriesColors = series.map { extras.compareColor(it.colorIndex, primary, tertiary) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
    ) {
        if (series.isEmpty() || series.all { it.points.isEmpty() }) return@Canvas

        val padding = 8.dp.toPx()
        val w = size.width - padding * 2
        val h = size.height - padding * 2
        val zeroY = padding + h / 2f

        val maxGap = series.flatMap { it.points }.maxOfOrNull { abs(it.gapSeconds) }?.coerceAtLeast(1.0) ?: 1.0
        val maxDistance = series.flatMap { it.points }.maxOfOrNull { it.distanceMeters }?.coerceAtLeast(1.0) ?: 1.0

        drawLine(
            color = zeroLineColor,
            start = Offset(padding, zeroY),
            end = Offset(padding + w, zeroY),
            strokeWidth = 1.dp.toPx(),
        )

        series.forEachIndexed { seriesIndex, s ->
            if (s.points.isEmpty()) return@forEachIndexed
            val color = seriesColors[seriesIndex]
            val path = Path()
            s.points.forEachIndexed { index, point ->
                val x = padding + (point.distanceMeters / maxDistance).toFloat() * w
                val y = zeroY - (point.gapSeconds / maxGap).toFloat() * (h / 2f)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
