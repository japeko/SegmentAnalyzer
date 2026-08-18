package com.segmentanalyzer.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.segmentanalyzer.common.DispatcherProvider
import com.segmentanalyzer.common.format.toRideCardDate
import com.segmentanalyzer.data.local.gpx.GpxFileParser
import com.segmentanalyzer.data.local.gpx.GpxParseException
import com.segmentanalyzer.data.local.gpx.GpxTrackSummary
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.GpxFileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.time.Duration
import javax.inject.Inject

internal class GpxFileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gpxFileParser: GpxFileParser,
    private val dispatcherProvider: DispatcherProvider,
) : GpxFileRepository {

    override suspend fun parseGpxFile(uri: String): Result<Ride> =
        withContext(dispatcherProvider.io) {
            runCatching {
                val parsedUri = Uri.parse(uri)
                val summary = context.contentResolver.openInputStream(parsedUri)?.use { gpxFileParser.parse(it) }
                    ?: throw GpxParseException("couldn't open the selected file")
                summary.toRide(uri, displayNameFor(parsedUri))
            }
        }

    private fun displayNameFor(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}

private fun GpxTrackSummary.toRide(uri: String, displayName: String?): Ride {
    val activityType = toActivityTypeOrNull() ?: throw GpxParseException("this GPX file isn't a cycling activity")
    val cleanName = displayName?.removeSuffix(".gpx")?.removeSuffix(".GPX")?.trim()?.ifBlank { null }
    return Ride(
        id = 0,
        name = cleanName ?: name ?: "Cycling – ${startTime.toRideCardDate()}",
        activityType = activityType,
        source = ActivitySource.GPX_FILE,
        startTime = startTime,
        duration = Duration.between(startTime, endTime),
        distanceMeters = distanceMeters,
        elevationGainMeters = elevationGainMeters,
        isPersonalBest = false,
        elevationProfile = emptyList(),
        sourceFilePath = uri,
    )
}

/**
 * Unlike FIT's always-present `Sport` enum, GPX's `<trk><type>` is free text and often absent
 * entirely — so a missing hint defaults to allowing the import (as [ActivityType.OTHER]) rather
 * than rejecting it, and only a hint that clearly names a non-cycling activity is rejected.
 */
private fun GpxTrackSummary.toActivityTypeOrNull(): ActivityType? {
    val hint = activityTypeHint?.lowercase() ?: return ActivityType.OTHER
    val nonCyclingKeywords = listOf("run", "walk", "hik", "swim", "ski")
    if (nonCyclingKeywords.any { hint.contains(it) }) return null
    return when {
        hint.contains("mountain") || hint.contains("mtb") -> ActivityType.MTB
        hint.contains("gravel") || hint.contains("cross") -> ActivityType.GRAVEL
        hint.contains("road") -> ActivityType.ROAD
        else -> ActivityType.OTHER
    }
}
