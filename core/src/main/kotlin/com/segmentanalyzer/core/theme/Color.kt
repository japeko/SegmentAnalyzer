package com.segmentanalyzer.core.theme

import androidx.compose.ui.graphics.Color

// Dark theme — converted from the design mockups' OKLCH tokens.
val DarkBackground = Color(0xFF15161C)
val DarkSurface = Color(0xFF1E1F26)
val DarkSurfaceVariant = Color(0xFF292B33)
val DarkOutline = Color(0xFF3D3F49)
val DarkOnSurface = Color(0xFFF5F5F7)
val DarkOnSurfaceSecondary = Color(0xFFB7B9C3)
val DarkOnSurfaceTertiary = Color(0xFF83858F)
val DarkPrimary = Color(0xFF8A7CF0) // brand violet
val DarkOnPrimary = Color(0xFF1E1B33)
val DarkTertiary = Color(0xFFE8C468) // personal-best amber
val DarkOnTertiary = Color(0xFF2E2409)
val DarkError = Color(0xFFEF9A9A)
val DarkOnError = Color(0xFF3A0A0A)
val DarkFaster = Color(0xFF6FB8E8) // ahead-of-reference delta
val DarkSlower = Color(0xFFE8935D) // behind-reference delta
val DarkCompareC = Color(0xFF5FC4B8) // third ride-comparison slot (teal)

// Dracula theme — the standard Dracula palette (draculatheme.com/contribute), mapped onto the
// same roles as Dark/Light so every screen picks it up for free.
val DraculaBackground = Color(0xFF282A36)
val DraculaSurface = Color(0xFF2F3244)
val DraculaSurfaceVariant = Color(0xFF44475A) // "Current Line"
val DraculaOutline = Color(0xFF6272A4) // "Comment"
val DraculaOnSurface = Color(0xFFF8F8F2) // "Foreground"
val DraculaOnSurfaceSecondary = Color(0xFFC2C3D6)
val DraculaOnSurfaceTertiary = Color(0xFF9A9CBF)
val DraculaPrimary = Color(0xFFBD93F9) // "Purple"
val DraculaOnPrimary = Color(0xFF282A36)
val DraculaTertiary = Color(0xFFF1FA8C) // "Yellow" — personal-best accent
val DraculaOnTertiary = Color(0xFF282A36)
val DraculaError = Color(0xFFFF5555) // "Red"
val DraculaOnError = Color(0xFF282A36)
val DraculaFaster = Color(0xFF8BE9FD) // "Cyan" — ahead-of-reference delta
val DraculaSlower = Color(0xFFFFB86C) // "Orange" — behind-reference delta
val DraculaCompareC = Color(0xFF50FA7B) // "Green" — third ride-comparison slot

// Trailhead theme — sampled from the app icon (about_hero.png): its stopwatch orange and sky
// blue on the deep navy the sky gradient fades into, with the icon's muted teal-green forest as
// the third comparison slot.
val TrailheadBackground = Color(0xFF0A1C33)
val TrailheadSurface = Color(0xFF122B4A)
val TrailheadSurfaceVariant = Color(0xFF1D3B5C)
val TrailheadOutline = Color(0xFF3D5F7D)
val TrailheadOnSurface = Color(0xFFF3EFE2)
val TrailheadOnSurfaceSecondary = Color(0xFFB8C7D9)
val TrailheadOnSurfaceTertiary = Color(0xFF8098B0)
val TrailheadPrimary = Color(0xFFEF6923) // stopwatch/trail orange
val TrailheadOnPrimary = Color(0xFF1A0D02)
val TrailheadTertiary = Color(0xFF29A3F5) // sky blue — personal-best accent
val TrailheadOnTertiary = Color(0xFF04162B)
val TrailheadError = Color(0xFFE5484D)
val TrailheadOnError = Color(0xFF1A0704)
val TrailheadFaster = Color(0xFF60CEF9) // ahead-of-reference delta
val TrailheadSlower = Color(0xFFC96A4E) // behind-reference delta
val TrailheadCompareC = Color(0xFF7FB8A8) // third ride-comparison slot (icon's forest teal)

// Light theme
val LightBackground = Color(0xFFFAFAFB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F0F3)
val LightOutline = Color(0xFFDDDEE2)
val LightOnSurface = Color(0xFF2B2D33)
val LightOnSurfaceSecondary = Color(0xFF595C66)
val LightOnSurfaceTertiary = Color(0xFF7B7E87)
val LightPrimary = Color(0xFF5B4FC7)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightTertiary = Color(0xFFB08D2E)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightError = Color(0xFFB3261E)
val LightOnError = Color(0xFFFFFFFF)
val LightFaster = Color(0xFF3B7FC4) // ahead-of-reference delta
val LightSlower = Color(0xFFC4622E) // behind-reference delta
val LightCompareC = Color(0xFF2E9184) // third ride-comparison slot (teal)

// Lavender theme — Light's palette with a visible tint of the brand violet on
// background/surfaceVariant instead of neutral white, so it doesn't read as a generic default
// light theme. Surface itself stays pure white so cards still pop against the tinted background;
// every other role is identical to Light.
val LavenderBackground = Color(0xFFE3DCF5)
val LavenderSurface = Color(0xFFFFFFFF)
val LavenderSurfaceVariant = Color(0xFFCFC2EA)
val LavenderOutline = Color(0xFFB3A1DC)
val LavenderOnSurface = LightOnSurface
val LavenderOnSurfaceSecondary = LightOnSurfaceSecondary
val LavenderOnSurfaceTertiary = LightOnSurfaceTertiary
val LavenderPrimary = LightPrimary
val LavenderOnPrimary = LightOnPrimary
val LavenderTertiary = LightTertiary
val LavenderOnTertiary = LightOnTertiary
val LavenderError = LightError
val LavenderOnError = LightOnError
val LavenderFaster = LightFaster
val LavenderSlower = LightSlower
val LavenderCompareC = LightCompareC
