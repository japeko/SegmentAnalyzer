package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.RideComparisonAttemptSummary
import com.segmentanalyzer.domain.model.RideComparisonGapPoint
import com.segmentanalyzer.domain.model.RideComparisonSummary
import com.segmentanalyzer.domain.repository.RideComparisonInsightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val summary = RideComparisonSummary(
    segmentName = "Skyline Ridge Climb",
    segmentDistanceMeters = 1_500.0,
    referenceLabel = "Current ride (2026-08-16)",
    attempts = listOf(
        RideComparisonAttemptSummary(
            label = "Current ride (2026-08-16)",
            durationSeconds = 312,
            avgSpeedKmh = 18.5,
            avgPowerWatts = 210.0,
            finalGapSeconds = null,
            worstPoint = null,
            bestPoint = null,
        ),
        RideComparisonAttemptSummary(
            label = "Personal Best (2026-06-01)",
            durationSeconds = 288,
            avgSpeedKmh = 20.1,
            avgPowerWatts = null,
            finalGapSeconds = -24.0,
            // An early loss (smaller magnitude) that's partly clawed back later by a bigger gain —
            // both points must survive into the prompt, not just whichever has the bigger number.
            worstPoint = RideComparisonGapPoint(distanceMeters = 150.0, gapSeconds = 3.0),
            bestPoint = RideComparisonGapPoint(distanceMeters = 900.0, gapSeconds = -18.5),
        ),
    ),
)

class GenerateRideComparisonInsightUseCaseTest {

    @Test
    fun `builds a prompt naming the segment and every compared ride, and returns the repository's result`() = runTest {
        val repository = FakeInsightRepository(Result.success("You lost the most time on the middle climb."))
        val useCase = GenerateRideComparisonInsightUseCase(repository)

        val result = useCase(summary)

        assertEquals("You lost the most time on the middle climb.", result.getOrNull())
        val prompt = repository.lastPrompt.orEmpty()
        assertTrue(prompt.contains("Skyline Ridge Climb"))
        assertTrue(prompt.contains("Current ride (2026-08-16)"))
        assertTrue(prompt.contains("Personal Best (2026-06-01)"))
        assertTrue(prompt.contains("210 W"))
        // The reference attempt has no gap fields — nothing to compare it against itself.
        assertTrue(prompt.lines().first { it.contains("Current ride") }.let { !it.contains("behind") && !it.contains("ahead") })
        // The comparison itself (who's fastest, and by how much) is precomputed as a stated fact
        // rather than left for the model to work out from the raw numbers — see the prompt's doc.
        assertTrue(prompt.contains("FACT: Personal Best (2026-06-01) was the fastest, at 4:48 — 24s quicker than Current ride (2026-08-16)."))
        // The reference's own biggest loss and gain are both stated explicitly, from the
        // reference's point of view — not left for the model to infer from the other ride's
        // ahead/behind data, which produced answers that missed this entirely in testing.
        assertTrue(
            prompt.contains(
                "FACT: Current ride (2026-08-16)'s biggest loss was around 900m into the segment, " +
                    "where it fell about 19s behind Personal Best (2026-06-01).",
            ),
        )
        assertTrue(
            prompt.contains(
                "FACT: Current ride (2026-08-16)'s biggest gain was around 150m into the segment, " +
                    "where it pulled about 3s ahead of Personal Best (2026-06-01).",
            ),
        )
    }

    @Test
    fun `surfaces a repository failure`() = runTest {
        val repository = FakeInsightRepository(Result.failure(IllegalStateException("model busy")))
        val useCase = GenerateRideComparisonInsightUseCase(repository)

        val result = useCase(summary)

        assertEquals("model busy", result.exceptionOrNull()?.message)
    }
}

private class FakeInsightRepository(private val result: Result<String>) : RideComparisonInsightRepository {
    var lastPrompt: String? = null

    override fun observeAvailability(): Flow<Boolean> = MutableStateFlow(true)

    override suspend fun generateInsight(prompt: String): Result<String> {
        lastPrompt = prompt
        return result
    }
}
