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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.feature.segments.detail.ProgressPoint

/** One point per attempt, chronological, faster times plotted higher. Same normalize→Path technique as ElevationSparkline. */
@Composable
fun ProgressChart(points: List<ProgressPoint>, highlightedAttemptId: Long? = null, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val prColor = MaterialTheme.colorScheme.tertiary
    val highlightColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
    ) {
        if (points.size < 2) return@Canvas

        val padding = 8.dp.toPx()
        val w = size.width - padding * 2
        val h = size.height - padding * 2
        val stepX = w / (points.size - 1)

        fun pointAt(index: Int): Offset {
            val y = points[index].normalizedY.coerceIn(0f, 1f)
            return Offset(x = padding + stepX * index, y = padding + h - y * h)
        }

        val path = Path().apply {
            moveTo(pointAt(0).x, pointAt(0).y)
            for (i in 1 until points.size) lineTo(pointAt(i).x, pointAt(i).y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        points.forEachIndexed { index, point ->
            val offset = pointAt(index)
            val isHighlighted = point.attemptId == highlightedAttemptId
            when {
                isHighlighted -> {
                    drawCircle(color = highlightColor.copy(alpha = 0.25f), radius = 9.dp.toPx(), center = offset)
                    drawCircle(color = highlightColor, radius = 6.dp.toPx(), center = offset)
                }
                point.rank == 1 -> drawCircle(color = prColor, radius = 5.dp.toPx(), center = offset)
                point.rank == 2 -> drawCircle(color = SilverMedal, radius = 5.dp.toPx(), center = offset)
                point.rank == 3 -> drawCircle(color = BronzeMedal, radius = 5.dp.toPx(), center = offset)
                else -> drawCircle(color = lineColor, radius = 3.dp.toPx(), center = offset)
            }
        }
    }
}
