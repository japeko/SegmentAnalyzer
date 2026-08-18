package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.GarminConnectionState
import com.segmentanalyzer.domain.repository.GarminAccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Current Garmin Connect connection state, for display in Settings. */
class ObserveGarminConnectionStateUseCase @Inject constructor(
    private val garminAccountRepository: GarminAccountRepository,
) {
    operator fun invoke(): Flow<GarminConnectionState> = garminAccountRepository.observeConnectionState()
}
