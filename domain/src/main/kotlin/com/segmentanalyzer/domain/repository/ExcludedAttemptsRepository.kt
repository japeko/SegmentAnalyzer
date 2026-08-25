package com.segmentanalyzer.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Attempts the rider has manually excluded from a segment's Progress Over Time chart and "All
 * Attempts" list — e.g. a known-bad or irrelevant lap they'd rather not compare against, without
 * deleting the underlying data.
 */
interface ExcludedAttemptsRepository {
    fun observeExcludedAttemptIds(): Flow<Set<Long>>

    suspend fun setExcluded(attemptId: Long, excluded: Boolean)
}
