package com.segmentanalyzer.feature.history.history.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.core.theme.NumericFontFamily
import com.segmentanalyzer.core.ui.ElevationSparkline
import com.segmentanalyzer.core.ui.PbBadge
import com.segmentanalyzer.core.ui.SourceTag
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.feature.history.history.RideListItem
import kotlin.math.roundToInt

@Composable
fun RideCard(item: RideListItem, onClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    val borderColor = if (item.isPersonalBest) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(item.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            ElevationSparkline(profile = item.elevationProfile)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    SourceTag(source = item.source)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Terrain,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(11.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${item.activityType.label()} · ${item.dateLabel}",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val stats = "%.1f km · %s · %d m · %.1f km/h".format(
                        item.distanceKm,
                        item.durationLabel,
                        item.elevationGainMeters.roundToInt(),
                        item.avgSpeedKmh,
                    )
                    Text(
                        text = stats,
                        fontFamily = NumericFontFamily,
                        fontSize = 12.sp,
                        color = MaterialThemeExtras.textTertiary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.isPersonalBest) {
                        PbBadge()
                    }
                }
            }
        }
    }
}

private fun ActivityType.label(): String = when (this) {
    ActivityType.MTB -> "MTB"
    ActivityType.GRAVEL -> "Gravel"
    ActivityType.ROAD -> "Road"
    ActivityType.OTHER -> "Other"
}
