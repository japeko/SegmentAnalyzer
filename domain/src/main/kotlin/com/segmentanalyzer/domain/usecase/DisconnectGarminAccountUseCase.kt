package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.GarminAccountRepository
import javax.inject.Inject

/** Clears the locally stored Garmin Connect session. */
class DisconnectGarminAccountUseCase @Inject constructor(
    private val garminAccountRepository: GarminAccountRepository,
) {
    suspend operator fun invoke() = garminAccountRepository.disconnect()
}
