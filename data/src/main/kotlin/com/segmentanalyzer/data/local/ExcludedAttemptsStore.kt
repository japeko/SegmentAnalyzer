package com.segmentanalyzer.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** On-device storage for which segment attempt ids the rider has excluded from comparison. */
@Singleton
internal class ExcludedAttemptsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _excludedAttemptIds = MutableStateFlow(readExcludedAttemptIds())
    val excludedAttemptIds: StateFlow<Set<Long>> = _excludedAttemptIds.asStateFlow()

    fun setExcluded(attemptId: Long, excluded: Boolean) {
        val updated = if (excluded) _excludedAttemptIds.value + attemptId else _excludedAttemptIds.value - attemptId
        prefs.edit().putStringSet(KEY_EXCLUDED_IDS, updated.map { it.toString() }.toSet()).apply()
        _excludedAttemptIds.value = updated
    }

    private fun readExcludedAttemptIds(): Set<Long> =
        prefs.getStringSet(KEY_EXCLUDED_IDS, null)?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()

    private companion object {
        const val PREFS_NAME = "excluded_attempts"
        const val KEY_EXCLUDED_IDS = "excluded_attempt_ids"
    }
}
