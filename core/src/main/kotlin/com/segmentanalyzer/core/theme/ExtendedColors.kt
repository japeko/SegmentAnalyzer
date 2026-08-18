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
)

private val LocalExtendedColors = compositionLocalOf {
    ExtendedColors(textTertiary = DarkOnSurfaceTertiary)
}

val MaterialThemeExtras: ExtendedColors
    @Composable
    get() = LocalExtendedColors.current

@Composable
fun ProvideExtendedColors(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val extended = if (darkTheme) {
        ExtendedColors(textTertiary = DarkOnSurfaceTertiary)
    } else {
        ExtendedColors(textTertiary = LightOnSurfaceTertiary)
    }
    CompositionLocalProvider(LocalExtendedColors provides extended, content = content)
}
