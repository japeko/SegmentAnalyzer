package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

/** On-device app preferences that aren't tied to any imported data. */
interface SettingsRepository {
    fun observeThemePreference(): Flow<ThemePreference>

    suspend fun setThemePreference(preference: ThemePreference)
}
