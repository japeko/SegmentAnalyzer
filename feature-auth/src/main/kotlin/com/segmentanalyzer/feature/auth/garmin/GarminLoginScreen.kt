package com.segmentanalyzer.feature.auth.garmin

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarminLoginScreen(
    uiState: GarminLoginUiState,
    onWebViewUrlChanged: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var retryCount by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Connect Garmin") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.signInUrl.isNotBlank() && !uiState.isConnected) {
                GarminSignInWebView(
                    url = uiState.signInUrl,
                    reloadKey = retryCount,
                    onUrlChanged = onWebViewUrlChanged,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (uiState.isExchangingToken) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = "Finishing sign-in…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }

            if (uiState.errorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.onErrorContainer)
                    Button(onClick = { retryCount++ }, modifier = Modifier.fillMaxWidth()) {
                        Text("Try again")
                    }
                }
            }
        }
    }
}

/**
 * A WebView showing Garmin's real sign-in page — Cloudflare's bot-detection JS challenge on that
 * page needs a real browser engine, which raw HTTP requests can't provide. [reloadKey] changing
 * forces a brand new WebView instance (and so a fresh sign-in attempt), since a used ticket can't
 * be replayed — this is inside `key()` rather than relying on `AndroidView`'s `update`, since
 * `update` re-runs on any unrelated recomposition (e.g. the loading overlay toggling), which would
 * otherwise reload the page on every state change, not just an explicit retry.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun GarminSignInWebView(
    url: String,
    reloadKey: Int,
    onUrlChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    key(reloadKey) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            url?.let(onUrlChanged)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            // Garmin's sign-in URL still carries id=gauth-widget — confirmed live
                            // that this serves the *embeddable widget* markup (meant to sit inside
                            // a small container on a host page), a fixed narrow width left-aligned
                            // in body, not a normal responsive mobile page. useWideViewPort /
                            // loadWithOverviewMode don't touch this — it's not a viewport-scaling
                            // issue. Center it directly instead.
                            view?.evaluateJavascript(
                                "document.body.style.display='flex';" +
                                    "document.body.style.flexDirection='column';" +
                                    "document.body.style.alignItems='center';",
                                null,
                            )
                        }

                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            url?.let(onUrlChanged)
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                            val requestUrl = request?.url?.toString() ?: return false
                            requestUrl.let(onUrlChanged)
                            // A CAS ticket is single-use: if we let the WebView keep navigating,
                            // Garmin's own client-side JS (confirmed live — it bounces
                            // /modern -> /app) redeems the ticket itself first, so our own
                            // OAuth1 exchange 401s with "ticket not recognized". Stop the WebView
                            // right here so we get first (and only) use of it.
                            val isTicketUrl = requestUrl.contains("ticket=")
                            if (isTicketUrl) Log.d("GarminSso", "WebView reached a ticket URL, halting further navigation: $requestUrl")
                            return isTicketUrl
                        }
                    }
                    loadUrl(url)
                }
            },
        )
    }
}
