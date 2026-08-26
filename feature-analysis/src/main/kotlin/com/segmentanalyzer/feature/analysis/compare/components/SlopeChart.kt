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
import com.segmentanalyzer.domain.util.SlopePoint
import kotlin.math.abs
import kotlin.math.roundToInt

private val CHART_HEIGHT = 90.dp
private val CHART_PADDING = 8.dp

/**
 * A single grade%-vs-distance line — slope is a property of the route, not of any one ride, so
 * unlike [SpeedChart]/[TimeGapChart] there's exactly one line regardless of how many attempts are
 * being compared. Shares the same drag-to-scrub shape as those charts.
 */
@Composable
fun SlopeChart(
    points: List<SlopePoint>,
    selectedFraction: Float? = null,
    onFractionSelected: (Float?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val zeroLineColor = MaterialTheme.colorScheme.onSurfaceVariant
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
        val maxGrade = points.maxOfOrNull { abs(it.gradePercent) }?.coerceAtLeast(1.0) ?: 1.0
        val maxDistance = points.maxOfOrNull { it.distanceMeters }?.coerceAtLeast(1.0) ?: 1.0

        fun yFor(gradePercent: Double): Float = zeroY - (gradePercent / maxGrade).toFloat() * (h / 2f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (points.isEmpty()) return@Canvas

            drawLine(
                color = zeroLineColor.copy(alpha = 0.4f),
                start = Offset(paddingPx, zeroY),
                end = Offset(paddingPx + w, zeroY),
                strokeWidth = 1.5.dp.toPx(),
            )

            val path = Path()
            points.forEachIndexed { index, point ->
                val x = paddingPx + (point.distanceMeters / maxDistance).toFloat() * w
                val y = yFor(point.gradePercent)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

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

        if (selectedFraction != null && points.isNotEmpty()) {
            val fraction = selectedFraction.coerceIn(0f, 1f)
            val x = paddingPx + fraction * w
            val grade = interpolatedGradeAt(points, fraction * maxDistance)
            if (grade != null) {
                Text(
                    text = "%+.1f%%".format(grade),
                    color = lineColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.offset { IntOffset((x + 6f).roundToInt(), (yFor(grade) - 22f).roundToInt()) },
                )
            }
        }
    }
}

/** Linear interpolation of [points]' gradePercent at [targetDistance] — points are ordered by distanceMeters. */
private fun interpolatedGradeAt(points: List<SlopePoint>, targetDistance: Double): Double? {
    if (points.isEmpty()) return null
    if (targetDistance <= points.first().distanceMeters) return points.first().gradePercent
    if (targetDistance >= points.last().distanceMeters) return points.last().gradePercent

    for (i in 0 until points.size - 1) {
        val a = points[i]
        val b = points[i + 1]
        if (targetDistance in a.distanceMeters..b.distanceMeters) {
            val span = b.distanceMeters - a.distanceMeters
            val t = if (span > 0.0) (targetDistance - a.distanceMeters) / span else 0.0
            return a.gradePercent + (b.gradePercent - a.gradePercent) * t
        }
    }
    return points.last().gradePercent
}
