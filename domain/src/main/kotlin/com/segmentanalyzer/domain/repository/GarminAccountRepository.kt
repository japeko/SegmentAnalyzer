package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.GarminConnectionState
import kotlinx.coroutines.flow.Flow

/**
 * Manages the local session linking this device to a Garmin Connect account.
 *
 * Garmin's sign-in page is behind Cloudflare's bot-detection JS challenge, which a plain HTTP
 * client can't satisfy — so sign-in happens in a WebView showing Garmin's real page (the rider
 * types their password there, not into this app's own UI), and this repository only takes over
 * once that flow reaches its completion URL.
 */
interface GarminAccountRepository {
    /** Current connection state, updated as the account connects/disconnects. */
    fun observeConnectionState(): Flow<GarminConnectionState>

    /** The username from the last connect attempt (success or not), to pre-fill the login form. Never the password. */
    fun lastUsername(): String?

    /** The URL to load in a WebView for the rider to sign in on Garmin's own page. */
    fun signInUrl(): String

    /** True once [url] (observed as the WebView navigates) signals a completed Garmin sign-in. */
    fun isSignInComplete(url: String): Boolean

    /**
     * Finishes a sign-in whose completion URL satisfied [isSignInComplete], and stores the
     * resulting session. [username] is for local display only ("Connected as ___").
     */
    suspend fun completeSignIn(username: String, completionUrl: String): Result<Unit>

    /** Clears the locally stored Garmin Connect session. */
    suspend fun disconnect()
}
