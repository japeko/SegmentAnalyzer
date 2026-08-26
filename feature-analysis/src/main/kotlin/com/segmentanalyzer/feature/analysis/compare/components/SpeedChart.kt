package com.segmentanalyzer.feature.analysis.compare.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.domain.usecase.SpeedPoint
import com.segmentanalyzer.feature.analysis.compare.SpeedSeriesUi
import kotlin.math.roundToInt

private val CHART_HEIGHT = 110.dp
private val CHART_PADDING = 8.dp

/**
 * One line per attempt's speed-vs-distance curve (absolute, unlike [TimeGapChart]'s relative
 * gap — there's no single "reference" line here). Shares the same drag-to-scrub shape as
 * [TimeGapChart] so a [selectedFraction] set from either chart keeps both, and the route map, in
 * sync.
 */
@Composable
fun SpeedChart(
    series: List<SpeedSeriesUi>,
    selectedFraction: Float? = null,
    onFractionSelected: (Float?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val extras = MaterialThemeExtras
    val seriesColors = series.map { extras.compareColor(it.colorIndex, primary, tertiary) }
    val selectionLineColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .pointerInput(Unit) {
                val padding = CHART_PADDING.toPx()
                fun fractionForX(x: Float): Float {
                    val w = size.width - padding * 2
                    if (w <= 0f) return 0f
                    return ((x - padding) / w).coerceIn(0f, 1f)
                }
                // Same reasoning as TimeGapChart: claim the gesture from the very first touch so a
                // non-perfectly-horizontal drag doesn't lose out to the parent list's own scroll.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    onFractionSelected(fractionForX(down.position.x))
                    drag(down.id) { change ->
                        onFractionSelected(fractionForX(change.position.x))
                        change.consume()
                    }
                    onFractionSelected(null)
                }
            },
    ) {
        val paddingPx = with(density) { CHART_PADDING.toPx() }
        val w = with(density) { maxWidth.toPx() } - paddingPx * 2
        val h = with(density) { maxHeight.toPx() } - paddingPx * 2
        val maxSpeed = series.flatMap { it.points }.maxOfOrNull { it.speedKmh }?.coerceAtLeast(1.0) ?: 1.0
        val maxDistance = series.flatMap { it.points }.maxOfOrNull { it.distanceMeters }?.coerceAtLeast(1.0) ?: 1.0

        fun yFor(speedKmh: Double): Float = paddingPx + h - (speedKmh / maxSpeed).toFloat() * h

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (series.isEmpty() || series.all { it.points.isEmpty() }) return@Canvas

            series.forEachIndexed { seriesIndex, s ->
                if (s.points.isEmpty()) return@forEachIndexed
                val color = seriesColors[seriesIndex]
                val path = Path()
                s.points.forEachIndexed { index, point ->
                    val x = paddingPx + (point.distanceMeters / maxDistance).toFloat() * w
                    val y = yFor(point.speedKmh)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path = path, color = color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            selectedFraction?.let { fraction ->
                val x = paddingPx + fraction.coerceIn(0f, 1f) * w
                drawLine(
                    color = selectionLineColor.copy(alpha = 0.5f),
                    start = Offset(x, paddingPx),
                    end = Offset(x, paddingPx + h),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
        }

        if (selectedFraction != null && series.any { it.points.isNotEmpty() }) {
            val fraction = selectedFraction.coerceIn(0f, 1f)
            val x = paddingPx + fraction * w

            series.forEachIndexed { seriesIndex, s ->
                val speed = interpolatedSpeedAt(s.points, fraction * maxDistance) ?: return@forEachIndexed
                SpeedLabel(text = "%.1f".format(speed), color = seriesColors[seriesIndex], x = x, y = yFor(speed))
            }
        }
    }
}

@Composable
private fun SpeedLabel(text: String, color: Color, x: Float, y: Float) {
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = Modifier.offset { IntOffset((x + 6f).roundToInt(), (y - 22f).roundToInt()) },
    )
}

/** Linear interpolation of [points]' speedKmh at [targetDistance] — points are ordered by distanceMeters. */
private fun interpolatedSpeedAt(points: List<SpeedPoint>, targetDistance: Double): Double? {
    if (points.isEmpty()) return null
    if (targetDistance <= points.first().distanceMeters) return points.first().speedKmh
    if (targetDistance >= points.last().distanceMeters) return points.last().speedKmh

    for (i in 0 until points.size - 1) {
        val a = points[i]
        val b = points[i + 1]
        if (targetDistance in a.distanceMeters..b.distanceMeters) {
            val span = b.distanceMeters - a.distanceMeters
            val t = if (span > 0.0) (targetDistance - a.distanceMeters) / span else 0.0
            return a.speedKmh + (b.speedKmh - a.speedKmh) * t
        }
    }
    return points.last().speedKmh
}
