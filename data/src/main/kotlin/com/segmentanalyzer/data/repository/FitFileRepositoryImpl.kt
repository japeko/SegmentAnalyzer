package com.segmentanalyzer.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.garmin.fit.Sport
import com.garmin.fit.SubSport
import com.segmentanalyzer.common.DispatcherProvider
import com.segmentanalyzer.common.format.toRideCardDate
import com.segmentanalyzer.data.local.fit.FitFileParser
import com.segmentanalyzer.data.local.fit.FitParseException
import com.segmentanalyzer.data.local.fit.FitSessionSummary
import com.segmentanalyzer.data.local.fit.FitTrackPoint
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.FitFileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

internal class FitFileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fitFileParser: FitFileParser,
    private val dispatcherProvider: DispatcherProvider,
) : FitFileRepository {

    override suspend fun parseFitFile(uri: String): Result<Ride> =
        withContext(dispatcherProvider.io) {
            runCatching {
                val parsedUri = Uri.parse(uri)
                val summary = context.contentResolver.openInputStream(parsedUri)?.use { fitFileParser.parse(it) }
                    ?: throw FitParseException("couldn't open the selected file")
                summary.toRide(uri, displayNameFor(parsedUri))
            }
        }

    private fun displayNameFor(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}

private fun FitSessionSummary.toRide(uri: String, displayName: String?): Ride {
    val activityType = toActivityTypeOrNull() ?: throw FitParseException("this FIT file isn't a cycling activity")
    val resolvedStartTime = startTime ?: Instant.now()
    val cleanName = displayName?.removeSuffix(".fit")?.removeSuffix(".FIT")?.trim()?.ifBlank { null }
    return Ride(
        id = 0,
        name = cleanName ?: "Cycling – ${resolvedStartTime.toRideCardDate()}",
        activityType = activityType,
        source = ActivitySource.FIT_FILE,
        startTime = resolvedStartTime,
        duration = Duration.ofMillis(((durationSeconds ?: 0f) * 1000).toLong()),
        distanceMeters = (distanceMeters ?: 0f).toDouble(),
        elevationGainMeters = (elevationGainMeters ?: 0).toDouble(),
        isPersonalBest = false,
        elevationProfile = emptyList(),
        sourceFilePath = uri,
        track = trackPoints.map { it.toDomain() },
    )
}

private fun FitTrackPoint.toDomain(): TrackPoint = TrackPoint(
    latitude = latitude,
    longitude = longitude,
    elevationMeters = elevationMeters,
    timestamp = timestamp,
    cumulativeDistanceMeters = cumulativeDistanceMeters,
    heartRateBpm = heartRateBpm,
    cadenceRpm = cadenceRpm,
    powerWatts = powerWatts,
)

/** Maps FIT's Sport/SubSport to ours, or null if it isn't a cycling activity at all. */
private fun FitSessionSummary.toActivityTypeOrNull(): ActivityType? {
    if (sport != Sport.CYCLING && sport != Sport.E_BIKING) return null
    return when (subSport) {
        SubSport.MOUNTAIN, SubSport.DOWNHILL -> ActivityType.MTB
        SubSport.CYCLOCROSS -> ActivityType.GRAVEL
        SubSport.ROAD -> ActivityType.ROAD
        else -> ActivityType.OTHER
    }
}
