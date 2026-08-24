package com.segmentanalyzer.data.repository

import com.segmentanalyzer.data.local.ThemePreferenceStore
import com.segmentanalyzer.domain.model.ThemePreference
import com.segmentanalyzer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class SettingsRepositoryImpl @Inject constructor(
    private val themePreferenceStore: ThemePreferenceStore,
) : SettingsRepository {

    override fun observeThemePreference(): Flow<ThemePreference> = themePreferenceStore.preference

    override suspend fun setThemePreference(preference: ThemePreference) = themePreferenceStore.save(preference)
}
