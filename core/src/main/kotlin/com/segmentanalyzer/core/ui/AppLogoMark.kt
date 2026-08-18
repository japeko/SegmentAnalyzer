package com.segmentanalyzer.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's mark: a rising zigzag (an elevation profile with a split point) on a rounded,
 * brand-colored square. Drawn with [Canvas] rather than a hand-authored vector asset — quicker
 * to iterate on than crafting SVG path data by hand.
 */
@Composable
fun AppLogoMark(modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val brandOn = MaterialTheme.colorScheme.onPrimary
    Canvas(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(size * 0.28f)),
    ) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val p1 = Offset(w * 0.18f, h * 0.68f)
        val p2 = Offset(w * 0.40f, h * 0.34f)
        val p3 = Offset(w * 0.56f, h * 0.55f)
        val p4 = Offset(w * 0.82f, h * 0.18f)

        drawLine(color = brandOn, start = p1, end = p2, strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = brandOn, start = p2, end = p3, strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = brandOn, start = p3, end = p4, strokeWidth = stroke.width, cap = stroke.cap)
        drawCircle(color = brandOn, radius = w * 0.06f, center = p3)
    }
}
