package com.segmentanalyzer.data.local

import android.content.Context
import com.segmentanalyzer.domain.model.ThemePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** On-device storage for the rider's chosen [ThemePreference] — not sensitive, so plain prefs. */
@Singleton
internal class ThemePreferenceStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _preference = MutableStateFlow(readPreference())
    val preference: StateFlow<ThemePreference> = _preference.asStateFlow()

    fun save(preference: ThemePreference) {
        prefs.edit().putString(KEY_THEME, preference.name).apply()
        _preference.value = preference
    }

    private fun readPreference(): ThemePreference {
        val name = prefs.getString(KEY_THEME, null) ?: return ThemePreference.SYSTEM
        return runCatching { ThemePreference.valueOf(name) }.getOrDefault(ThemePreference.SYSTEM)
    }

    private companion object {
        const val PREFS_NAME = "settings"
        const val KEY_THEME = "theme_preference"
    }
}
