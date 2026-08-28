package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.common.format.toRideClock
import com.segmentanalyzer.domain.model.RideComparisonAttemptSummary
import com.segmentanalyzer.domain.model.RideComparisonSummary
import com.segmentanalyzer.domain.repository.RideComparisonInsightRepository
import java.time.Duration
import javax.inject.Inject

/**
 * Turns a [RideComparisonSummary] into a short natural-language explanation of why one ride was
 * faster than the others, via the on-device model. Callers must confirm
 * [ObserveRideComparisonInsightAvailabilityUseCase] emits true first — this doesn't check availability itself.
 */
class GenerateRideComparisonInsightUseCase @Inject constructor(
    private val repository: RideComparisonInsightRepository,
) {
    suspend operator fun invoke(summary: RideComparisonSummary): Result<String> =
        repository.generateInsight(summary.toPrompt())
}

/**
 * On-device models this small are unreliable at comparing raw numbers themselves — asking Gemini
 * Nano "explain why the fastest ride was faster" and letting it work out *which one* that is from
 * a list of durations produced confidently wrong answers in testing (e.g. calling the slower ride
 * faster). So the comparison itself is done here in code — the only thing the model is asked to
 * do is narrate a fact it's handed, not compute one.
 */
private fun RideComparisonSummary.toPrompt(): String {
    val fastest = attempts.minByOrNull { it.durationSeconds } ?: return ""
    val reference = attempts.find { it.label == referenceLabel }
    val fastestTime = Duration.ofSeconds(fastest.durationSeconds).toRideClock()

    return buildString {
        appendLine("Segment: $segmentName (${segmentDistanceMeters.toInt()} m)")
        appendLine("Reference ride: $referenceLabel")
        appendLine()
        appendLine("Rides compared, fastest first:")
        attempts.sortedBy { it.durationSeconds }.forEach { appendLine("- ${it.toLine()}") }
        appendLine()
        if (reference != null && reference.label != fastest.label) {
            val secondsFaster = reference.durationSeconds - fastest.durationSeconds
            appendLine("FACT: ${fastest.label} was the fastest, at $fastestTime — ${secondsFaster}s quicker than $referenceLabel.")
        } else {
            appendLine("FACT: ${fastest.label} was the fastest of these rides, at $fastestTime.")
        }
        // bestPoint on a non-reference attempt is the point where THAT attempt was furthest ahead
        // of the reference — i.e. exactly where the reference itself fell furthest behind. Stated
        // explicitly from the reference's own point of view ("you lost..."), since leaving the
        // model to infer that reframing from "X was ahead of Y" data on its own has produced
        // answers that missed it entirely in testing.
        val referenceWorstMoment = attempts
            .mapNotNull { attempt -> attempt.bestPoint?.let { attempt.label to it } }
            .maxByOrNull { (_, point) -> kotlin.math.abs(point.gapSeconds) }
        if (referenceWorstMoment != null) {
            val (otherLabel, point) = referenceWorstMoment
            appendLine(
                "FACT: $referenceLabel's biggest loss was around ${point.distanceMeters.toInt()}m into the " +
                    "segment, where it fell about ${"%.0f".format(kotlin.math.abs(point.gapSeconds))}s behind $otherLabel.",
            )
        }
        // The mirror image: worstPoint on a non-reference attempt is where THAT attempt fell
        // furthest behind the reference — i.e. where the reference itself gained the most ground.
        val referenceBestMoment = attempts
            .mapNotNull { attempt -> attempt.worstPoint?.let { attempt.label to it } }
            .maxByOrNull { (_, point) -> kotlin.math.abs(point.gapSeconds) }
        if (referenceBestMoment != null) {
            val (otherLabel, point) = referenceBestMoment
            appendLine(
                "FACT: $referenceLabel's biggest gain was around ${point.distanceMeters.toInt()}m into the " +
                    "segment, where it pulled about ${"%.0f".format(point.gapSeconds)}s ahead of $otherLabel.",
            )
        }
        appendLine()
        append(
            "In 2-3 short sentences, explain why ${fastest.label} was faster than the others on this " +
                "segment, referencing where along the segment time was gained or lost. Speak directly " +
                "to the rider as \"you\". Do not repeat the raw numbers back verbatim.",
        )
    }
}

private fun RideComparisonAttemptSummary.toLine(): String {
    val time = Duration.ofSeconds(durationSeconds).toRideClock()
    val speed = "%.1f km/h".format(avgSpeedKmh)
    val power = avgPowerWatts?.let { ", avg %.0f W".format(it) }.orEmpty()
    val gap = finalGapSeconds?.let {
        val sign = if (it <= 0) "ahead" else "behind"
        " — %.1fs %s of the reference at the finish".format(kotlin.math.abs(it), sign)
    }.orEmpty()
    return "$label: $time, $speed$power$gap"
}
