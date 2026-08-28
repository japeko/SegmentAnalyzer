package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.SegmentRecord
import com.segmentanalyzer.domain.repository.FitExportRepository
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import java.io.File
import javax.inject.Inject

/**
 * [exportedFiles] one per record that had a recorded track to export; [skippedCount] is how many
 * of the requested records didn't (e.g. a Garmin-sourced attempt with no GPS matching and no
 * Strava effort data) and were silently left out.
 */
data class FitExportResult(val exportedFiles: List<File>, val skippedCount: Int)

/**
 * Exports each of [records] as its own .fit file. A record's track lives at the segment-attempt
 * level, not the whole ride — see [SegmentAttemptRepository.trackPointsForAttempt] — so each
 * export is just that record-setting lap, not the entire original ride.
 */
class ExportRecordsToFitUseCase @Inject constructor(
    private val segmentAttemptRepository: SegmentAttemptRepository,
    private val fitExportRepository: FitExportRepository,
) {
    suspend operator fun invoke(records: List<SegmentRecord>): FitExportResult {
        val exported = mutableListOf<File>()
        var skipped = 0
        records.forEach { record ->
            val points = segmentAttemptRepository.trackPointsForAttempt(record.attemptId)
            if (points.isEmpty()) {
                skipped++
            } else {
                exported += fitExportRepository.exportRecord(record, points)
            }
        }
        return FitExportResult(exported, skipped)
    }
}
