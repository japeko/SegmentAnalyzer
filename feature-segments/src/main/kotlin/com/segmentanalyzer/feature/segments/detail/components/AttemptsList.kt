package com.segmentanalyzer.feature.segments.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.segmentanalyzer.feature.segments.detail.AttemptItem

@Composable
fun AttemptRow(item: AttemptItem, onClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    val borderColor = if (item.isPersonalBest) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
    val containerColor = if (item.isPersonalBest) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(item.id) },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
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
