package com.segmentanalyzer.data.repository

import android.util.Log
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.generationConfig
import com.segmentanalyzer.common.DispatcherProvider
import com.segmentanalyzer.domain.repository.RideComparisonInsightRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RideComparisonInsight"

/**
 * Wraps ML Kit's on-device Gemini Nano Prompt API. A phone that's capable but hasn't downloaded
 * the model yet ([FeatureStatus.DOWNLOADABLE]/`DOWNLOADING`) silently starts (or reattaches to) a
 * background download the first time [observeAvailability] is collected, in [applicationScope] so
 * it survives the caller's screen closing — [isAvailable] flips to true once that finishes.
 */
@Singleton
internal class RideComparisonInsightRepositoryImpl @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val applicationScope: CoroutineScope,
) : RideComparisonInsightRepository {

    private val model: GenerativeModel by lazy { Generation.getClient(generationConfig {}) }
    private val isAvailable = MutableStateFlow(false)
    private var downloadJob: Job? = null

    override fun observeAvailability(): Flow<Boolean> = isAvailable.onStart { refreshAvailability() }

    private suspend fun refreshAvailability() = withContext(dispatcherProvider.io) {
        val result = runCatching { model.checkStatus() }
        result.exceptionOrNull()?.let { Log.w(TAG, "checkStatus() failed", it) }
        val status = result.getOrNull()
        Log.d(
            TAG,
            "checkStatus() = $status " +
                "(AVAILABLE=${FeatureStatus.AVAILABLE}, DOWNLOADABLE=${FeatureStatus.DOWNLOADABLE}, " +
                "DOWNLOADING=${FeatureStatus.DOWNLOADING}, UNAVAILABLE=${FeatureStatus.UNAVAILABLE})",
        )
        when (status) {
            FeatureStatus.AVAILABLE -> isAvailable.value = true
            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> startDownloadIfNeeded()
            else -> Unit
        }
    }

    private fun startDownloadIfNeeded() {
        if (downloadJob != null) return
        downloadJob = applicationScope.launch(dispatcherProvider.io) {
            runCatching {
                model.download().collect { status ->
                    when (status) {
                        is DownloadStatus.DownloadCompleted -> {
                            Log.d(TAG, "Gemini Nano download completed")
                            isAvailable.value = true
                        }
                        is DownloadStatus.DownloadFailed -> Log.w(TAG, "Gemini Nano download failed", status.e)
                        else -> Unit
                    }
                }
            }.onFailure { Log.w(TAG, "Gemini Nano download threw", it) }
        }
    }

    override suspend fun generateInsight(prompt: String): Result<String> = withContext(dispatcherProvider.io) {
        Log.d(TAG, "generateInsight() prompt:\n$prompt")
        runCatching {
            val response = model.generateContent(prompt)
            val text = response.candidates.firstOrNull()?.text?.trim()
            require(!text.isNullOrEmpty()) { "Gemini Nano returned no text" }
            text
        }.onSuccess { Log.d(TAG, "generateInsight() response: $it") }
            .onFailure { Log.w(TAG, "generateInsight() failed", it) }
    }
}
