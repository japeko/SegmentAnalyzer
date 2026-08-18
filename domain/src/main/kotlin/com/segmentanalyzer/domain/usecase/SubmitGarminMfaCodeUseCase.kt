package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.GarminAccountRepository
import javax.inject.Inject

/** Completes a Garmin Connect login that required a multi-factor auth code. */
class SubmitGarminMfaCodeUseCase @Inject constructor(
    private val garminAccountRepository: GarminAccountRepository,
) {
    suspend operator fun invoke(code: String): Result<Unit> =
        garminAccountRepository.submitMfaCode(code)
}
