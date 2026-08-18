package com.segmentanalyzer.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.unit.sp

/**
 * The design mockups pair Roboto (UI text) with Roboto Condensed (numeric/data fields —
 * distances, times, speeds). Both ship as part of AOSP's system font set under the
 * "sans-serif"/"sans-serif-condensed" family aliases, so they're referenced by name here
 * instead of bundling font files: no extra APK weight, no network fetch (Downloadable Fonts
 * would need one), consistent with the app being offline-first.
 */
val NumericFontFamily = FontFamily(
    Font(DeviceFontFamilyName("sans-serif-condensed"), weight = FontWeight.Normal),
    Font(DeviceFontFamilyName("sans-serif-condensed-medium"), weight = FontWeight.Medium),
    Font(DeviceFontFamilyName("sans-serif-condensed"), weight = FontWeight.Bold),
)

/** For distances, durations, speeds — anywhere tabular figures matter. */
val NumericTextStyle = TextStyle(
    fontFamily = NumericFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
)

val SegmentAnalyzerTypography = Typography()
