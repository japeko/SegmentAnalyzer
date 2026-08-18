package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.GarminAccountRepository
import javax.inject.Inject

/** The username from the last Garmin Connect login attempt, to pre-fill the login form. */
class GetLastGarminUsernameUseCase @Inject constructor(
    private val garminAccountRepository: GarminAccountRepository,
) {
    operator fun invoke(): String? = garminAccountRepository.lastUsername()
}
