package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.StravaAccountRepository
import javax.inject.Inject

/** Clears the locally stored Strava session. */
class DisconnectStravaAccountUseCase @Inject constructor(
    private val stravaAccountRepository: StravaAccountRepository,
) {
    suspend operator fun invoke() = stravaAccountRepository.disconnect()
}
