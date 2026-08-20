package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.GarminAccountRepository
import javax.inject.Inject

/** True once [url] (observed as the sign-in WebView navigates) signals a completed Garmin sign-in. */
class IsGarminSignInCompleteUseCase @Inject constructor(
    private val garminAccountRepository: GarminAccountRepository,
) {
    operator fun invoke(url: String): Boolean = garminAccountRepository.isSignInComplete(url)
}
