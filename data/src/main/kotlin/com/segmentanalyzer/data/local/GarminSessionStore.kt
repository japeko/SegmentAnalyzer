package com.segmentanalyzer.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.segmentanalyzer.data.remote.garmin.GarminSession
import com.segmentanalyzer.domain.model.GarminConnectionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted on-device storage for the Garmin Connect OAuth2 session. The password used to obtain
 * this session is never passed here or persisted anywhere.
 */
@Singleton
internal class GarminSessionStore @Inject constructor(
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
    val state: StateFlow<GarminConnectionState> = _state.asStateFlow()

    fun save(session: GarminSession) {
        prefs.edit()
            .putString(KEY_USERNAME, session.username)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_EXPIRES_AT, session.expiresAt.epochSecond)
            .putLong(KEY_CONNECTED_AT, Instant.now().epochSecond)
            .apply()
        _state.value = readState()
    }

    fun clear() {
        prefs.edit().clear().apply()
        _state.value = GarminConnectionState.Disconnected
    }

    /** The stored session, for authenticated Garmin API calls (e.g. ride import). */
    fun session(): GarminSession? {
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        return GarminSession(
            username = username,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = Instant.ofEpochSecond(prefs.getLong(KEY_EXPIRES_AT, 0L)),
        )
    }

    private fun readState(): GarminConnectionState {
        val username = prefs.getString(KEY_USERNAME, null)
            ?: return GarminConnectionState.Disconnected
        return GarminConnectionState.Connected(
            username = username,
            connectedAt = Instant.ofEpochSecond(prefs.getLong(KEY_CONNECTED_AT, 0L)),
        )
    }

    private companion object {
        const val PREFS_NAME = "garmin_session"
        const val KEY_USERNAME = "username"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_CONNECTED_AT = "connected_at"
    }
}
