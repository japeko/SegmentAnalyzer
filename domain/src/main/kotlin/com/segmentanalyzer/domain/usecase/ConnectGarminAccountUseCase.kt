package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.GarminAccountRepository
import javax.inject.Inject

/** Logs in to Garmin Connect and stores the resulting session on-device. */
class ConnectGarminAccountUseCase @Inject constructor(
    private val garminAccountRepository: GarminAccountRepository,
) {
    suspend operator fun invoke(username: String, password: String): Result<Unit> =
        garminAccountRepository.connect(username, password)
}
