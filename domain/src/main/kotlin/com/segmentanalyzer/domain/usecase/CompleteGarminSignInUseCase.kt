package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.GarminAccountRepository
import javax.inject.Inject

/** Finishes a Garmin Connect sign-in using the WebView's completion URL, and stores the session. */
class CompleteGarminSignInUseCase @Inject constructor(
    private val garminAccountRepository: GarminAccountRepository,
) {
    suspend operator fun invoke(username: String, completionUrl: String): Result<Unit> =
        garminAccountRepository.completeSignIn(username, completionUrl)
}
