package com.segmentanalyzer.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The mockups use a three-tier text hierarchy (primary/secondary/tertiary) where Material3's
 * ColorScheme only has two (onSurface/onSurfaceVariant). This carries the third tier alongside
 * the theme rather than hardcoding a color in every composable that needs it.
 */
data class ExtendedColors(
    val textTertiary: Color,
    /** Delta coloring for attempt/comparison charts: ahead of reference. */
    val faster: Color,
    /** Delta coloring for attempt/comparison charts: behind reference. */
    val slower: Color,
    /** Third slot color for multi-ride comparison chips/charts (first two reuse primary/tertiary). */
    val compareC: Color,
) {
    /** Cycles through the comparison palette (primary, tertiary, compareC, ...) for chip N. */
    fun compareColor(index: Int, primary: Color, tertiary: Color): Color =
        when (index % 3) {
            0 -> primary
            1 -> tertiary
            else -> compareC
        }
}

private val LocalExtendedColors = compositionLocalOf {
    ExtendedColors(textTertiary = DarkOnSurfaceTertiary, faster = DarkFaster, slower = DarkSlower, compareC = DarkCompareC)
}

val MaterialThemeExtras: ExtendedColors
    @Composable
    get() = LocalExtendedColors.current

@Composable
fun ProvideExtendedColors(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val extended = if (darkTheme) {
        ExtendedColors(textTertiary = DarkOnSurfaceTertiary, faster = DarkFaster, slower = DarkSlower, compareC = DarkCompareC)
    } else {
        ExtendedColors(textTertiary = LightOnSurfaceTertiary, faster = LightFaster, slower = LightSlower, compareC = LightCompareC)
    }
    CompositionLocalProvider(LocalExtendedColors provides extended, content = content)
}
