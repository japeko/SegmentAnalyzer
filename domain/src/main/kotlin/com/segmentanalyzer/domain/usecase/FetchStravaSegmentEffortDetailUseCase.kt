package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.StravaSegmentEffortDetail
import com.segmentanalyzer.domain.repository.StravaActivityRepository
import javax.inject.Inject

/** Fetches pace/power/HR/cadence summary stats for one specific Strava segment effort. */
class FetchStravaSegmentEffortDetailUseCase @Inject constructor(
    private val stravaActivityRepository: StravaActivityRepository,
) {
    suspend operator fun invoke(effortExternalId: String): Result<StravaSegmentEffortDetail> =
        stravaActivityRepository.fetchEffortDetail(effortExternalId)
}
