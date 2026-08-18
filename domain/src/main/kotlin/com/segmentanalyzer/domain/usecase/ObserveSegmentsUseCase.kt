package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Segment
import com.segmentanalyzer.domain.repository.SegmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Segments for the segments list screen. */
class ObserveSegmentsUseCase @Inject constructor(
    private val segmentRepository: SegmentRepository,
) {
    operator fun invoke(): Flow<List<Segment>> = segmentRepository.observeSegments()
}
