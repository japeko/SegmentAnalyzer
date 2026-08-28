package com.segmentanalyzer.data.repository

import android.content.Context
import com.segmentanalyzer.common.DispatcherProvider
import com.segmentanalyzer.data.local.fit.FitExportPoint
import com.segmentanalyzer.data.local.fit.FitFileEncoder
import com.segmentanalyzer.domain.model.SegmentRecord
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.FitExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** Written under the app's cache dir, in a subfolder the FileProvider config exposes for sharing — see file_paths.xml. */
private const val EXPORTS_SUBDIR = "exports"

internal class FitExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fitFileEncoder: FitFileEncoder,
    private val dispatcherProvider: DispatcherProvider,
) : FitExportRepository {

    override suspend fun exportRecord(record: SegmentRecord, points: List<TrackPoint>): File =
        withContext(dispatcherProvider.io) {
            val exportsDir = File(context.cacheDir, EXPORTS_SUBDIR).apply { mkdirs() }
            val file = File(exportsDir, "${fileNameFor(record)}.fit")
            fitFileEncoder.encode(file, points.map { it.toExportPoint() })
            file
        }
}

private fun fileNameFor(record: SegmentRecord): String {
    val safeSegmentName = record.segmentName.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifEmpty { "segment" }
    return "${safeSegmentName}_${record.startTime.toEpochMilli()}"
}

private fun TrackPoint.toExportPoint() = FitExportPoint(
    latitude = latitude,
    longitude = longitude,
    elevationMeters = elevationMeters,
    timestamp = timestamp,
    cumulativeDistanceMeters = cumulativeDistanceMeters,
    heartRateBpm = heartRateBpm,
    cadenceRpm = cadenceRpm,
    powerWatts = powerWatts,
)
