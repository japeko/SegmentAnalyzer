package com.segmentanalyzer.feature.history.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
import com.segmentanalyzer.feature.history.detail.StravaSegmentEffortItem

@Composable
fun StravaSegmentEffortRow(item: StravaSegmentEffortItem, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().clickable { onClick(item.segmentExternalId) }) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = item.segmentName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    item.prRank?.let { rank ->
                        Text(
                            text = if (rank == 1) "PR" else "PR #$rank",
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    item.komRank?.let { rank ->
                        Text(
                            text = "KOM #$rank",
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                Text(
                    text = "%.1f km".format(item.distanceKm),
                    fontSize = 11.sp,
                    color = MaterialThemeExtras.textTertiary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Text(
                text = item.elapsedTimeLabel,
                fontFamily = NumericFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}
