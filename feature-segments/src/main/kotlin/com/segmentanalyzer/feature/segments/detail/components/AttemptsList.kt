package com.segmentanalyzer.feature.segments.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.core.theme.NumericFontFamily
import com.segmentanalyzer.feature.segments.detail.AttemptItem

@Composable
fun AttemptRow(
    item: AttemptItem,
    isSelected: Boolean,
    onClick: (Long) -> Unit,
    onHoverChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    LaunchedEffect(isHovered) { onHoverChange(isHovered) }

    val borderColor = when {
        isHovered || isSelected -> MaterialTheme.colorScheme.primary
        item.isPersonalBest -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }
    val containerColor = when {
        isHovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        item.isPersonalBest -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = { onClick(item.id) },
        modifier = modifier.fillMaxWidth().hoverable(interactionSource),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isHovered || isSelected) 2.dp else 1.dp, borderColor),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = item.dateLabel, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    if (item.isPersonalBest) {
                        Text(
                            text = "PR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    if (item.isFromStrava) {
                        Text(
                            text = "STRAVA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = MaterialThemeExtras.textTertiary,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                Text(
                    text = item.lapLabel,
                    fontSize = 11.sp,
                    color = MaterialThemeExtras.textTertiary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = item.durationLabel, fontFamily = NumericFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (!item.isPersonalBest) {
                    Text(
                        text = "+%ds".format(item.deltaVsPrSeconds),
                        fontFamily = NumericFontFamily,
                        fontSize = 11.sp,
                        color = MaterialThemeExtras.slower,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

/** An [AttemptRow] in "All Attempts" — swipe left to exclude it from the chart and this list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcludableAttemptRow(
    item: AttemptItem,
    isSelected: Boolean,
    onClick: (Long) -> Unit,
    onHoverChange: (Boolean) -> Unit,
    onExcluded: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onExcluded(item.id)
            true
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = { SwipeActionBackground(alignEnd = true, icon = Icons.Filled.VisibilityOff, label = "Exclude") },
    ) {
        AttemptRow(item = item, isSelected = isSelected, onClick = onClick, onHoverChange = onHoverChange)
    }
}

/** An [AttemptRow] in the excluded section — swipe right to restore it to "All Attempts" and the chart. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncludableAttemptRow(
    item: AttemptItem,
    onIncluded: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) onIncluded(item.id)
            true
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromEndToStart = false,
        backgroundContent = { SwipeActionBackground(alignEnd = false, icon = Icons.Filled.Visibility, label = "Restore") },
    ) {
        AttemptRow(item = item, isSelected = false, onClick = {}, onHoverChange = {})
    }
}

@Composable
private fun SwipeActionBackground(alignEnd: Boolean, icon: ImageVector, label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
            .padding(horizontal = 20.dp),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (alignEnd) {
                Text(text = label, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(start = 8.dp))
            } else {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(end = 8.dp))
                Text(text = label, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
