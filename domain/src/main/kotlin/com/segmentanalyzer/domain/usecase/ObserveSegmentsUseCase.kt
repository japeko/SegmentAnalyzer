package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Segment
import com.segmentanalyzer.domain.repository.SegmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Segments for the segments list screen — unfiltered by default, or narrowed to segments with
 * at least one attempt matching [tag] and/or the [afterEpochMillis]..[beforeEpochMillis) range
 * when any filter is passed (see [SegmentRepository.observeFilteredSegments]).
 */
class ObserveSegmentsUseCase @Inject constructor(
    private val segmentRepository: SegmentRepository,
) {
    operator fun invoke(
        tag: String? = null,
        afterEpochMillis: Long? = null,
        beforeEpochMillis: Long? = null,
    ): Flow<List<Segment>> = if (tag == null && afterEpochMillis == null && beforeEpochMillis == null) {
        segmentRepository.observeSegments()
    } else {
        segmentRepository.observeFilteredSegments(tag, afterEpochMillis, beforeEpochMillis)
    }
}
