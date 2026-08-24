package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.SegmentRecord
import com.segmentanalyzer.domain.model.SummaryPeriod
import com.segmentanalyzer.domain.model.isIn
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Every segment's current record, split by whether it was set within [SummaryPeriod] or earlier. */
data class SegmentRecordsSummary(
    val newPersonalBests: List<SegmentRecord>,
    val otherRecords: List<SegmentRecord>,
)

/**
 * Splits every segment's current record into ones set within [period] ("new PBs") and ones set
 * earlier ("other records"), both most-recent first.
 */
class ObserveSegmentRecordsUseCase @Inject constructor(
    private val segmentAttemptRepository: SegmentAttemptRepository,
) {
    operator fun invoke(period: SummaryPeriod): Flow<SegmentRecordsSummary> =
        segmentAttemptRepository.observeRecords().map { records ->
            val sorted = records.sortedByDescending { it.startTime }
            val (newOnes, older) = sorted.partition { it.startTime.isIn(period) }
            SegmentRecordsSummary(newPersonalBests = newOnes, otherRecords = older)
        }
}
