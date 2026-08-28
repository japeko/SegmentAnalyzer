package com.segmentanalyzer.feature.history.records.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import com.segmentanalyzer.core.ui.SourceTag
import com.segmentanalyzer.feature.history.records.RecordListItem

@Composable
fun RecordRow(
    item: RecordListItem,
    isNew: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isNew -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }
    val containerColor = if (isNew) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = null, modifier = Modifier.padding(end = 4.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isNew) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(end = 5.dp),
                        )
                    }
                    Text(text = item.segmentName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
                Text(
                    text = "%.1f km · %s · %s".format(item.distanceKm, item.rideName, item.dateLabel),
                    fontSize = 11.sp,
                    color = MaterialThemeExtras.textTertiary,
                    modifier = Modifier.padding(top = 2.dp),
                )
                SourceTag(source = item.rideSource, modifier = Modifier.padding(top = 4.dp))
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = item.durationLabel, fontFamily = NumericFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "%.1f km/h".format(item.avgSpeedKmh),
                    fontFamily = NumericFontFamily,
                    fontSize = 11.sp,
                    color = MaterialThemeExtras.textTertiary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
