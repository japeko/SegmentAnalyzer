package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.SegmentRecord
import com.segmentanalyzer.domain.model.TrackPoint
import java.io.File

/** Writes a [SegmentRecord]'s recorded track to a standalone .fit file in app-private storage, ready to share. */
interface FitExportRepository {
    suspend fun exportRecord(record: SegmentRecord, points: List<TrackPoint>): File
}
