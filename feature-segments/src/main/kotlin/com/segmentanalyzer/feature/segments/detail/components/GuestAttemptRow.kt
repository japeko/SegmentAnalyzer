package com.segmentanalyzer.feature.segments.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.core.theme.NumericFontFamily
import com.segmentanalyzer.feature.segments.detail.GuestAttemptItem

/** Imported from someone else's FIT file — visually distinct, and deliberately never eligible for PR/2nd/3rd rank. Swipe to delete, permanently (no restore section, unlike ExcludableAttemptRow). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestAttemptRow(item: GuestAttemptItem, onDelete: (Long) -> Unit, modifier: Modifier = Modifier) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) onDelete(item.id)
            true
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onError)
            }
        },
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialThemeExtras.textTertiary,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text(text = item.riderName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        Text(
                            text = "GUEST",
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = MaterialThemeExtras.textTertiary,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    Text(
                        text = item.dateLabel,
                        fontSize = 11.sp,
                        color = MaterialThemeExtras.textTertiary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
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
}
