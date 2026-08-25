package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.ExcludedAttemptsRepository
import javax.inject.Inject

class SetAttemptExcludedUseCase @Inject constructor(
    private val excludedAttemptsRepository: ExcludedAttemptsRepository,
) {
    suspend operator fun invoke(attemptId: Long, excluded: Boolean) =
        excludedAttemptsRepository.setExcluded(attemptId, excluded)
}
