package com.segmentanalyzer.feature.history.history.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.ui.AppLogoMark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideHistoryTopBar(onSearchClick: () -> Unit, modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(text = "Segment Analyzer", fontWeight = FontWeight.Bold)
        },
        navigationIcon = {
            AppLogoMark(modifier = Modifier.padding(start = 12.dp))
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = "Search rides")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(),
    )
}
