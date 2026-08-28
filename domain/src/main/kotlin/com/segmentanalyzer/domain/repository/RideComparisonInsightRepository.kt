package com.segmentanalyzer.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * On-device generative text explaining a ride comparison. Backed by Gemini Nano — nothing here
 * ever leaves the device. Only available on newer, high-end phones;
 * [observeAvailability] must emit true before [generateInsight] is called.
 */
interface RideComparisonInsightRepository {
    /**
     * Whether the on-device model is downloaded and ready on this phone. Re-checks on every new
     * collection, and silently starts a background download (surviving beyond that collection)
     * if the model is supported but not yet downloaded — this re-emits true once that finishes,
     * without the caller needing to do anything else.
     */
    fun observeAvailability(): Flow<Boolean>

    /** Generates a short natural-language explanation from [prompt], a plain-text description of the comparison. */
    suspend fun generateInsight(prompt: String): Result<String>
}
