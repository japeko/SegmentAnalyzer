package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.StravaAccountRepository
import javax.inject.Inject

/** The URL to open to start Strava's OAuth consent flow. */
class GetStravaAuthorizationUrlUseCase @Inject constructor(
    private val stravaAccountRepository: StravaAccountRepository,
) {
    operator fun invoke(): String = stravaAccountRepository.authorizationUrl()
}
