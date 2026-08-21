package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.StravaSegmentEffortDetail
import com.segmentanalyzer.domain.repository.StravaActivityRepository
import com.segmentanalyzer.domain.repository.StravaSegmentEffortRepository
import javax.inject.Inject

/**
 * Fetches pace/power/HR/cadence summary stats for one specific Strava segment effort and caches
 * it locally — this is the data used to compare that effort against the same segment on a
 * different ride, so it needs to persist rather than being re-fetched (and re-billed against
 * Strava's rate limit) every time.
 */
class FetchStravaSegmentEffortDetailUseCase @Inject constructor(
    private val stravaActivityRepository: StravaActivityRepository,
    private val stravaSegmentEffortRepository: StravaSegmentEffortRepository,
) {
    suspend operator fun invoke(effortExternalId: String): Result<StravaSegmentEffortDetail> =
        stravaActivityRepository.fetchEffortDetail(effortExternalId).onSuccess { detail ->
            stravaSegmentEffortRepository.saveEffortDetail(effortExternalId, detail)
        }
}
