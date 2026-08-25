package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.ExcludedAttemptsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveExcludedAttemptIdsUseCase @Inject constructor(
    private val excludedAttemptsRepository: ExcludedAttemptsRepository,
) {
    operator fun invoke(): Flow<Set<Long>> = excludedAttemptsRepository.observeExcludedAttemptIds()
}
