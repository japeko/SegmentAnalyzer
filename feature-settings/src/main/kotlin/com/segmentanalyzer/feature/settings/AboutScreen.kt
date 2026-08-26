package com.segmentanalyzer.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.segmentanalyzer.core.R
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.core.theme.NumericFontFamily
import com.segmentanalyzer.core.ui.FeatureRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
        ) {
            Hero()
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Text(
                text = "Deeper split analysis, gradient-aware maps, and ride-vs-ride comparison " +
                    "than Garmin Connect or Strava surface on their own — with analysis that runs " +
                    "entirely on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
            WhatItDoes()
            BuiltWith(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            PrivacyFooter(modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp))
            Text(
                text = "© 2026 Segment Analyzer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialThemeExtras.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            )
        }
    }
}

@Composable
private fun Hero(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 28.dp, start = 32.dp, end = 32.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.about_hero),
            contentDescription = "Segment Analyzer app icon",
            modifier = Modifier.size(112.dp).clip(RoundedCornerShape(26.dp)),
        )
        Text(
            text = "Segment Analyzer",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "VERSION 0.1.0",
            fontFamily = NumericFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = "Garmin records the ride. Strava handles the social layer.\n" +
                "Segment Analyzer explains why one ride was faster.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun WhatItDoes(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Text(
            text = "What It Does",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialThemeExtras.textTertiary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        FeatureRow(
            icon = Icons.Filled.Download,
            accent = MaterialTheme.colorScheme.primary,
            title = "Import from Garmin Connect",
            description = "Sign in once and your rides pull in automatically — no manual data entry.",
        )
        FeatureRow(
            icon = Icons.Filled.Place,
            accent = MaterialTheme.colorScheme.tertiary,
            title = "Automatic segment matching",
            description = "Finds every pass through a known segment, including multiple laps in one ride.",
        )
        FeatureRow(
            icon = Icons.AutoMirrored.Filled.CompareArrows,
            accent = MaterialTheme.colorScheme.primary,
            title = "Compare rides side by side",
            description = "A distance-aligned time-gap chart and gradient-colored map, synced as you scrub.",
        )
        FeatureRow(
            icon = Icons.Filled.CloudOff,
            accent = MaterialTheme.colorScheme.tertiary,
            title = "Offline-first",
            description = "Ride history and stats work with no network — import, map tiles, and a ride's live Strava segment list need one.",
        )
    }
}

@Composable
private fun BuiltWith(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "BUILT WITH",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialThemeExtras.textTertiary,
            )
            Text(
                text = "Kotlin · Jetpack Compose · Room · Coroutines/Flow · MapLibre",
                fontFamily = NumericFontFamily,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun PrivacyFooter(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(
            text = "Offline-first and privacy-first — your ride data stays on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
