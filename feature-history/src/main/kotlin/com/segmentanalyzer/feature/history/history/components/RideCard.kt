package com.segmentanalyzer.feature.history.history.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RideCard(
    item: RideListItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: (Long) -> Unit,
    onLongClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        item.isPersonalBest -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { onClick(item.id) }, onLongClick = { onLongClick(item.id) }),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = null, modifier = Modifier.padding(end = 4.dp))
            }

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
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                    if (!item.tag.isNullOrBlank()) {
                        Text(
                            text = item.tag,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
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
    ActivityType.EMTB -> "E-MTB"
    ActivityType.GRAVEL -> "Gravel"
    ActivityType.EGRAVEL -> "E-Gravel"
    ActivityType.ROAD -> "Road"
    ActivityType.EROAD -> "E-Road"
    ActivityType.OTHER -> "Other"
}
