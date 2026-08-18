package com.segmentanalyzer.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.domain.model.ActivitySource

/**
 * A plain text pill identifying where a ride was imported from — deliberately never a
 * recreated Garmin/Strava logo, just a label.
 */
@Composable
fun SourceTag(source: ActivitySource, modifier: Modifier = Modifier) {
    Text(
        text = source.label(),
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        letterSpacing = 0.3.sp,
        color = MaterialThemeExtras.textTertiary,
    )
}

private fun ActivitySource.label(): String = when (this) {
    ActivitySource.GARMIN -> "GARMIN"
    ActivitySource.FIT_FILE -> "FIT FILE"
    ActivitySource.GPX_FILE -> "GPX FILE"
    ActivitySource.STRAVA -> "STRAVA"
}
