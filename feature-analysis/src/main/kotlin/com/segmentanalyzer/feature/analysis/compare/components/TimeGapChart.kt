package com.segmentanalyzer.feature.analysis.compare.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.feature.analysis.compare.TimeGapSeriesUi
import kotlin.math.abs

/**
 * One line per non-Current attempt: seconds ahead (negative, below zero-line) or behind
 * (positive) Current, by distance. Current itself is always exactly 0 relative to itself, so it's
 * drawn as a flat line in its own chip color ([currentColorIndex]) rather than plotted as a series.
 *
 * Press-and-drag scrubs a vertical position indicator, reporting the touched distance fraction
 * (0f..1f) via [onFractionSelected] — the caller uses this to sync a marker on the route map.
 */
@Composable
fun TimeGapChart(
    series: List<TimeGapSeriesUi>,
    currentColorIndex: Int = 0,
    selectedFraction: Float? = null,
    onFractionSelected: (Float?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val extras = MaterialThemeExtras
    val zeroLineColor = extras.compareColor(currentColorIndex, primary, tertiary)
    val seriesColors = series.map { extras.compareColor(it.colorIndex, primary, tertiary) }
    val selectionLineColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .pointerInput(Unit) {
                val padding = 8.dp.toPx()
                fun fractionForX(x: Float): Float {
                    val w = size.width - padding * 2
                    if (w <= 0f) return 0f
                    return ((x - padding) / w).coerceIn(0f, 1f)
                }
                detectDragGestures(
                    onDragStart = { offset -> onFractionSelected(fractionForX(offset.x)) },
                    onDrag = { change, _ ->
                        onFractionSelected(fractionForX(change.position.x))
                        change.consume()
                    },
                    onDragEnd = { onFractionSelected(null) },
                    onDragCancel = { onFractionSelected(null) },
                )
            },
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
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
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

        selectedFraction?.let { fraction ->
            val x = padding + fraction.coerceIn(0f, 1f) * w
            drawLine(
                color = selectionLineColor.copy(alpha = 0.5f),
                start = Offset(x, padding),
                end = Offset(x, padding + h),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
    }
}
