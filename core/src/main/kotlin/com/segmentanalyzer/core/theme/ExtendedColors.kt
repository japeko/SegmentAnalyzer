package com.segmentanalyzer.core.theme

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
internal fun ProvideExtendedColors(variant: AppThemeVariant, content: @Composable () -> Unit) {
    val extended = when (variant) {
        AppThemeVariant.DARK ->
            ExtendedColors(textTertiary = DarkOnSurfaceTertiary, faster = DarkFaster, slower = DarkSlower, compareC = DarkCompareC)
        AppThemeVariant.LIGHT ->
            ExtendedColors(textTertiary = LightOnSurfaceTertiary, faster = LightFaster, slower = LightSlower, compareC = LightCompareC)
        AppThemeVariant.DRACULA ->
            ExtendedColors(
                textTertiary = DraculaOnSurfaceTertiary,
                faster = DraculaFaster,
                slower = DraculaSlower,
                compareC = DraculaCompareC,
            )
        AppThemeVariant.TRAILHEAD ->
            ExtendedColors(
                textTertiary = TrailheadOnSurfaceTertiary,
                faster = TrailheadFaster,
                slower = TrailheadSlower,
                compareC = TrailheadCompareC,
            )
    }
    CompositionLocalProvider(LocalExtendedColors provides extended, content = content)
}
