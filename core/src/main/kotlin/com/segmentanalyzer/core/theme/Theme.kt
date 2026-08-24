package com.segmentanalyzer.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.segmentanalyzer.domain.model.ThemePreference

private val DarkColors = darkColorScheme(
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceSecondary,
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimary,
    onPrimaryContainer = DarkOnPrimary,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiary,
    onTertiaryContainer = DarkOnTertiary,
    error = DarkError,
    onError = DarkOnError,
)

private val LightColors = lightColorScheme(
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceSecondary,
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimary,
    onPrimaryContainer = LightOnPrimary,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiary,
    onTertiaryContainer = LightOnTertiary,
    error = LightError,
    onError = LightOnError,
)

private val DraculaColors = darkColorScheme(
    background = DraculaBackground,
    surface = DraculaSurface,
    surfaceVariant = DraculaSurfaceVariant,
    outline = DraculaOutline,
    onBackground = DraculaOnSurface,
    onSurface = DraculaOnSurface,
    onSurfaceVariant = DraculaOnSurfaceSecondary,
    primary = DraculaPrimary,
    onPrimary = DraculaOnPrimary,
    primaryContainer = DraculaPrimary,
    onPrimaryContainer = DraculaOnPrimary,
    tertiary = DraculaTertiary,
    onTertiary = DraculaOnTertiary,
    tertiaryContainer = DraculaTertiary,
    onTertiaryContainer = DraculaOnTertiary,
    error = DraculaError,
    onError = DraculaOnError,
)

private val TrailheadColors = darkColorScheme(
    background = TrailheadBackground,
    surface = TrailheadSurface,
    surfaceVariant = TrailheadSurfaceVariant,
    outline = TrailheadOutline,
    onBackground = TrailheadOnSurface,
    onSurface = TrailheadOnSurface,
    onSurfaceVariant = TrailheadOnSurfaceSecondary,
    primary = TrailheadPrimary,
    onPrimary = TrailheadOnPrimary,
    primaryContainer = TrailheadPrimary,
    onPrimaryContainer = TrailheadOnPrimary,
    tertiary = TrailheadTertiary,
    onTertiary = TrailheadOnTertiary,
    tertiaryContainer = TrailheadTertiary,
    onTertiaryContainer = TrailheadOnTertiary,
    error = TrailheadError,
    onError = TrailheadOnError,
)

/** Which fixed palette [SegmentAnalyzerTheme] renders — [ThemePreference.SYSTEM] resolves to [LIGHT]/[DARK] before reaching here. */
internal enum class AppThemeVariant { LIGHT, DARK, DRACULA, TRAILHEAD }

/** The tertiary color role doubles as this app's "personal best" accent throughout. */
@Composable
fun SegmentAnalyzerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    SegmentAnalyzerTheme(variant = if (darkTheme) AppThemeVariant.DARK else AppThemeVariant.LIGHT, content = content)
}

/** Resolves the rider's [ThemePreference] (including [ThemePreference.DRACULA]/[ThemePreference.TRAILHEAD]) to a rendered theme. */
@Composable
fun SegmentAnalyzerTheme(themePreference: ThemePreference, content: @Composable () -> Unit) {
    val variant = when (themePreference) {
        ThemePreference.SYSTEM -> if (isSystemInDarkTheme()) AppThemeVariant.DARK else AppThemeVariant.LIGHT
        ThemePreference.LIGHT -> AppThemeVariant.LIGHT
        ThemePreference.DARK -> AppThemeVariant.DARK
        ThemePreference.DRACULA -> AppThemeVariant.DRACULA
        ThemePreference.TRAILHEAD -> AppThemeVariant.TRAILHEAD
    }
    SegmentAnalyzerTheme(variant = variant, content = content)
}

@Composable
private fun SegmentAnalyzerTheme(variant: AppThemeVariant, content: @Composable () -> Unit) {
    val colorScheme = when (variant) {
        AppThemeVariant.DARK -> DarkColors
        AppThemeVariant.LIGHT -> LightColors
        AppThemeVariant.DRACULA -> DraculaColors
        AppThemeVariant.TRAILHEAD -> TrailheadColors
    }
    ProvideExtendedColors(variant = variant) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SegmentAnalyzerTypography,
            content = content,
        )
    }
}
