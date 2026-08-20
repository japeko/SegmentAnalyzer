package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.GarminAccountRepository
import javax.inject.Inject

/** The URL to load in a WebView for the rider to sign in on Garmin's own page. */
class GetGarminSignInUrlUseCase @Inject constructor(
    private val garminAccountRepository: GarminAccountRepository,
) {
    operator fun invoke(): String = garminAccountRepository.signInUrl()
}
