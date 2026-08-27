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
import com.segmentanalyzer.domain.usecase.TimeGapPoint
import com.segmentanalyzer.feature.analysis.compare.TimeGapSeriesUi
import kotlin.math.abs
import kotlin.math.roundToInt

private val CHART_HEIGHT = 110.dp
private val CHART_PADDING = 8.dp

/**
 * One line per non-Current attempt: seconds ahead (negative, below zero-line) or behind
 * (positive) Current, by distance. Current itself is always exactly 0 relative to itself, so it's
 * drawn as a flat line in its own chip color ([currentColorIndex]) rather than plotted as a series.
 *
 * Press-and-drag scrubs a vertical position indicator, reporting the touched distance fraction
 * (0f..1f) via [onFractionSelected] — the caller uses this to sync a marker on the route map —
 * and shows each line's gap at that position (e.g. "-4s") next to it.
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
                // Claim the gesture on the very first touch (not detectDragGestures' touch-slop
                // start), and consume every move immediately — otherwise the parent LazyColumn's
                // vertical scroll wins any drag that isn't perfectly horizontal.
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
        val zeroY = paddingPx + h / 2f
        val maxGap = series.flatMap { it.points }.maxOfOrNull { abs(it.gapSeconds) }?.coerceAtLeast(1.0) ?: 1.0
        val maxDistance = series.flatMap { it.points }.maxOfOrNull { it.distanceMeters }?.coerceAtLeast(1.0) ?: 1.0

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (series.isEmpty() || series.all { it.points.isEmpty() }) return@Canvas

            drawLine(
                color = zeroLineColor,
                start = Offset(paddingPx, zeroY),
                end = Offset(paddingPx + w, zeroY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
            )

            series.forEachIndexed { seriesIndex, s ->
                if (s.points.isEmpty()) return@forEachIndexed
                val color = seriesColors[seriesIndex]
                val path = Path()
                s.points.forEachIndexed { index, point ->
                    val x = paddingPx + (point.distanceMeters / maxDistance).toFloat() * w
                    val y = zeroY - (point.gapSeconds / maxGap).toFloat() * (h / 2f)
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

            GapLabel(text = "0s", color = zeroLineColor, x = x, y = zeroY)

            series.forEachIndexed { seriesIndex, s ->
                val gap = interpolatedGapAt(s.points, fraction * maxDistance) ?: return@forEachIndexed
                val y = zeroY - (gap / maxGap).toFloat() * (h / 2f)
                GapLabel(text = formatGapSeconds(gap), color = seriesColors[seriesIndex], x = x, y = y)
            }
        }
    }
}

@Composable
private fun GapLabel(text: String, color: Color, x: Float, y: Float) {
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = Modifier.offset { IntOffset((x + 6f).roundToInt(), (y - 22f).roundToInt()) },
    )
}

/** Linear interpolation of [points]' gapSeconds at [targetDistance] — points are ordered by distanceMeters. */
private fun interpolatedGapAt(points: List<TimeGapPoint>, targetDistance: Double): Double? {
    if (points.isEmpty()) return null
    if (targetDistance <= points.first().distanceMeters) return points.first().gapSeconds
    if (targetDistance >= points.last().distanceMeters) return points.last().gapSeconds

    for (i in 0 until points.size - 1) {
        val a = points[i]
        val b = points[i + 1]
        if (targetDistance in a.distanceMeters..b.distanceMeters) {
            val span = b.distanceMeters - a.distanceMeters
            val t = if (span > 0.0) (targetDistance - a.distanceMeters) / span else 0.0
            return a.gapSeconds + (b.gapSeconds - a.gapSeconds) * t
        }
    }
    return points.last().gapSeconds
}

/**
 * One decimal place, not a whole-second round — the underlying gap is already sub-second precise
 * (linear interpolation over GPS-timestamped points, see BuildTimeGapSeriesUseCase), and rounding
 * to whole seconds made two genuinely different attempts both show "0s" for most of a segment,
 * since a sub-half-second gap is extremely common on a short climb/descent.
 */
private fun formatGapSeconds(gapSeconds: Double): String {
    val absSeconds = abs(gapSeconds)
    // Locale.ROOT: a comma-decimal locale (this app's own Finnish users included, e.g. "18,9 km"
    // elsewhere) would otherwise produce "0,4" here — fine to display, but a real crash risk if
    // that string is ever re-parsed with something like toDouble(), which requires a dot.
    val magnitude = "%.1f".format(java.util.Locale.ROOT, absSeconds)
    return when {
        absSeconds < 0.05 -> "0.0s"
        gapSeconds > 0 -> "+${magnitude}s"
        else -> "-${magnitude}s"
    }
}
