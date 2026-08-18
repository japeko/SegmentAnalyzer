package com.segmentanalyzer.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A small elevation-profile thumbnail for a ride card. [profile] is a normalized 0..1 series,
 * low values at the start of the list drawing near the bottom.
 */
@Composable
fun ElevationSparkline(profile: List<Float>, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fillColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        if (profile.size < 2) return@Canvas

        val w = this.size.width
        val h = this.size.height
        val stepX = w / (profile.size - 1)

        fun pointAt(index: Int): Offset {
            val normalized = profile[index].coerceIn(0f, 1f)
            return Offset(x = stepX * index, y = h - normalized * h)
        }

        val linePath = Path().apply {
            moveTo(pointAt(0).x, pointAt(0).y)
            for (i in 1 until profile.size) lineTo(pointAt(i).x, pointAt(i).y)
        }
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(path = fillPath, color = fillColor)
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
