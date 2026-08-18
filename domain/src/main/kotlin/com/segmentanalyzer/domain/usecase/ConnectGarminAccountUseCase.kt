package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.GarminConnectResult
import com.segmentanalyzer.domain.repository.GarminAccountRepository
import javax.inject.Inject

/** Logs in to Garmin Connect and stores the resulting session on-device, unless MFA is required. */
class ConnectGarminAccountUseCase @Inject constructor(
    private val garminAccountRepository: GarminAccountRepository,
) {
    suspend operator fun invoke(username: String, password: String): Result<GarminConnectResult> =
        garminAccountRepository.connect(username, password)
}
