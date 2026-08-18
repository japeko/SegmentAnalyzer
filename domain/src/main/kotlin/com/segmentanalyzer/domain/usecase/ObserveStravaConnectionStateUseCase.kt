package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.StravaConnectionState
import com.segmentanalyzer.domain.repository.StravaAccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Current Strava connection state, for display in Settings. */
class ObserveStravaConnectionStateUseCase @Inject constructor(
    private val stravaAccountRepository: StravaAccountRepository,
) {
    operator fun invoke(): Flow<StravaConnectionState> = stravaAccountRepository.observeConnectionState()
}
