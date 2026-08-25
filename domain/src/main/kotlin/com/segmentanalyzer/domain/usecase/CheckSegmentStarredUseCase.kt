package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.StravaSegmentRepository
import javax.inject.Inject

/** Whether the connected athlete currently has a segment starred on Strava. */
class CheckSegmentStarredUseCase @Inject constructor(
    private val stravaSegmentRepository: StravaSegmentRepository,
) {
    suspend operator fun invoke(segmentExternalId: String): Result<Boolean> =
        stravaSegmentRepository.isSegmentStarred(segmentExternalId)
}
