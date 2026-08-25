package com.segmentanalyzer.data.repository

import com.segmentanalyzer.data.local.ExcludedAttemptsStore
import com.segmentanalyzer.domain.repository.ExcludedAttemptsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class ExcludedAttemptsRepositoryImpl @Inject constructor(
    private val excludedAttemptsStore: ExcludedAttemptsStore,
) : ExcludedAttemptsRepository {

    override fun observeExcludedAttemptIds(): Flow<Set<Long>> = excludedAttemptsStore.excludedAttemptIds

    override suspend fun setExcluded(attemptId: Long, excluded: Boolean) =
        excludedAttemptsStore.setExcluded(attemptId, excluded)
}
