package com.segmentanalyzer.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.segmentanalyzer.data.remote.strava.StravaSession
import com.segmentanalyzer.domain.model.StravaConnectionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Encrypted on-device storage for the Strava OAuth2 session. */
@Singleton
internal class StravaSessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<StravaConnectionState> = _state.asStateFlow()

    fun save(session: StravaSession) {
        prefs.edit()
            .putString(KEY_ATHLETE_NAME, session.athleteName)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_EXPIRES_AT, session.expiresAt.epochSecond)
            .putLong(KEY_CONNECTED_AT, Instant.now().epochSecond)
            .apply()
        _state.value = readState()
    }

    fun clear() {
        prefs.edit().clear().apply()
        _state.value = StravaConnectionState.Disconnected
    }

    /** The stored session, for authenticated Strava API calls. */
    fun session(): StravaSession? {
        val athleteName = prefs.getString(KEY_ATHLETE_NAME, null) ?: return null
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        return StravaSession(
            athleteName = athleteName,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = Instant.ofEpochSecond(prefs.getLong(KEY_EXPIRES_AT, 0L)),
        )
    }

    private fun readState(): StravaConnectionState {
        val athleteName = prefs.getString(KEY_ATHLETE_NAME, null)
            ?: return StravaConnectionState.Disconnected
        return StravaConnectionState.Connected(
            athleteName = athleteName,
            connectedAt = Instant.ofEpochSecond(prefs.getLong(KEY_CONNECTED_AT, 0L)),
        )
    }

    private companion object {
        const val PREFS_NAME = "strava_session"
        const val KEY_ATHLETE_NAME = "athlete_name"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_CONNECTED_AT = "connected_at"
    }
}
