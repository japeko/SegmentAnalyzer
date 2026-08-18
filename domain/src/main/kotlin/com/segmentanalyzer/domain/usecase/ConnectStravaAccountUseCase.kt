package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.StravaAccountRepository
import javax.inject.Inject

/** Completes the Strava OAuth flow using the code from the redirect, and stores the session. */
class ConnectStravaAccountUseCase @Inject constructor(
    private val stravaAccountRepository: StravaAccountRepository,
) {
    suspend operator fun invoke(code: String): Result<Unit> = stravaAccountRepository.exchangeAuthorizationCode(code)
}
