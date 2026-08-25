package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.StravaSegmentRepository
import javax.inject.Inject

/** Stars (or unstars) a segment for the connected athlete on Strava. */
class SetSegmentStarredUseCase @Inject constructor(
    private val stravaSegmentRepository: StravaSegmentRepository,
) {
    suspend operator fun invoke(segmentExternalId: String, starred: Boolean): Result<Unit> =
        stravaSegmentRepository.setSegmentStarred(segmentExternalId, starred)
}
