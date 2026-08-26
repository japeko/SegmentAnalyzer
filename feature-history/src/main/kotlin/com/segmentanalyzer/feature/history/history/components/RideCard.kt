package com.segmentanalyzer.feature.history.history.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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

/**
 * A [RideListItem] card. Outside selection mode, swiping it left reveals "Delete" — swiping past
 * the threshold requests confirmation via [onDeleteRequested] rather than deleting immediately;
 * the card always snaps back into place, since the actual removal (if confirmed) happens through
 * the rest of the list re-rendering once the ride is gone, not through this swipe settling into a
 * dismissed state.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RideCard(
    item: RideListItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: (Long) -> Unit,
    onLongClick: (Long) -> Unit,
    onDeleteRequested: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isSelectionMode) {
        RideCardContent(
            item = item,
            isSelectionMode = true,
            isSelected = isSelected,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
        )
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDeleteRequested(item.id)
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Delete", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
    ) {
        RideCardContent(
            item = item,
            isSelectionMode = false,
            isSelected = isSelected,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RideCardContent(
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

            if (item.elevationProfile.size >= 2) {
                ElevationSparkline(profile = item.elevationProfile)
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(modifier = Modifier.weight(1f, fill = false), verticalAlignment = Alignment.CenterVertically) {
                        if (item.isViewed) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = "Already viewed",
                                tint = MaterialThemeExtras.textTertiary,
                                modifier = Modifier.padding(end = 5.dp).size(14.dp),
                            )
                        }
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                        )
                    }
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
