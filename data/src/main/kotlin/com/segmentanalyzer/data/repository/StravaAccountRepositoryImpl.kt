package com.segmentanalyzer.data.repository

import com.segmentanalyzer.common.DispatcherProvider
import com.segmentanalyzer.data.BuildConfig
import com.segmentanalyzer.data.local.StravaSessionStore
import com.segmentanalyzer.data.remote.strava.StravaAuthApi
import com.segmentanalyzer.data.remote.strava.StravaSession
import com.segmentanalyzer.domain.model.StravaConnectionState
import com.segmentanalyzer.domain.repository.StravaAccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.time.Instant
import javax.inject.Inject

internal class StravaAccountRepositoryImpl @Inject constructor(
    private val authApi: StravaAuthApi,
    private val sessionStore: StravaSessionStore,
    private val dispatcherProvider: DispatcherProvider,
) : StravaAccountRepository {

    override fun observeConnectionState(): Flow<StravaConnectionState> = sessionStore.state

    override fun authorizationUrl(): String {
        val encodedRedirect = URLEncoder.encode(REDIRECT_URI, "UTF-8")
        return "https://www.strava.com/oauth/authorize" +
            "?client_id=${BuildConfig.STRAVA_CLIENT_ID}" +
            "&redirect_uri=$encodedRedirect" +
            "&response_type=code" +
            "&approval_prompt=auto" +
            "&scope=read"
    }

    override suspend fun exchangeAuthorizationCode(code: String): Result<Unit> =
        withContext(dispatcherProvider.io) {
            runCatching {
                val response = authApi.exchangeCode(code)
                val athleteName = listOfNotNull(response.athlete?.firstname, response.athlete?.lastname)
                    .joinToString(" ")
                    .ifBlank { "Strava athlete" }
                sessionStore.save(
                    StravaSession(
                        athleteName = athleteName,
                        accessToken = response.accessToken,
                        refreshToken = response.refreshToken,
                        expiresAt = Instant.ofEpochSecond(response.expiresAt),
                    ),
                )
            }
        }

    override suspend fun disconnect() {
        withContext(dispatcherProvider.io) { sessionStore.clear() }
    }

    companion object {
        /** Must match the intent-filter in AndroidManifest.xml and the check in SegmentAnalyzerApp.kt. */
        const val REDIRECT_URI = "segmentanalyzer://localhost/strava-callback"
    }
}
