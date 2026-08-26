package com.segmentanalyzer.feature.segments.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.common.format.toRideCardDate
import com.segmentanalyzer.common.format.toRideClock
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.StravaConnectionState
import com.segmentanalyzer.domain.usecase.CheckSegmentStarredUseCase
import com.segmentanalyzer.domain.usecase.ObserveExcludedAttemptIdsUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentAttemptsUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentsUseCase
import com.segmentanalyzer.domain.usecase.ObserveStravaConnectionStateUseCase
import com.segmentanalyzer.domain.usecase.SetAttemptExcludedUseCase
import com.segmentanalyzer.domain.usecase.SetSegmentStarredUseCase
import com.segmentanalyzer.domain.util.lapLabelsByAttemptId
import com.segmentanalyzer.domain.util.routePoints
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SegmentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeSegments: ObserveSegmentsUseCase,
    observeSegmentAttempts: ObserveSegmentAttemptsUseCase,
    observeExcludedAttemptIds: ObserveExcludedAttemptIdsUseCase,
    observeStravaConnectionState: ObserveStravaConnectionStateUseCase,
    private val checkSegmentStarred: CheckSegmentStarredUseCase,
    private val setSegmentStarred: SetSegmentStarredUseCase,
    private val setAttemptExcluded: SetAttemptExcludedUseCase,
) : ViewModel() {

    private val segmentId: Long = checkNotNull(savedStateHandle["segmentId"])

    /** Non-null once we've confirmed via Strava that this segment isn't starred there yet. */
    private val starPromptState = MutableStateFlow<StarPromptState?>(null)

    /**
     * The attempt last tapped in "All Attempts", so its dot on the Progress Over Time chart stays
     * highlighted — a persistent marker of "what am I comparing" that survives navigating to
     * Compare Rides and back (unlike hover, which is transient composition-local state and isn't
     * available on touch anyway).
     */
    private val selectedAttemptId = MutableStateFlow<Long?>(null)

    /**
     * Display order for "All Attempts"/"Excluded" — lives here (not composition-local `remember`)
     * so it survives navigating to Compare Rides and back, same reason as [selectedAttemptId].
     */
    private val attemptsReversed = MutableStateFlow(false)

    /** The segment's external id, once resolved, so the star actions don't need a second lookup. */
    private var latestSegmentExternalId: String? = null

    init {
        // Skips the star-status check entirely (and reactively retries) while Strava isn't
        // connected, rather than firing a network call doomed to fail with an auth error every
        // time — same reasoning as RideDetailViewModel's Strava-effort fetch.
        viewModelScope.launch {
            val segment = observeSegments().map { segments -> segments.find { it.id == segmentId } }.first { it != null }
            checkNotNull(segment)
            observeStravaConnectionState().collect { state ->
                if (state is StravaConnectionState.Connected) {
                    checkSegmentStarred(segment.externalId).onSuccess { starred ->
                        if (!starred) starPromptState.value = StarPromptState()
                    }
                }
            }
        }
    }

    val uiState = combine(
        observeSegments().map { segments -> segments.find { it.id == segmentId } },
        observeSegmentAttempts(segmentId),
        starPromptState,
        selectedAttemptId,
        combine(observeExcludedAttemptIds(), attemptsReversed, observeStravaConnectionState()) { excludedIds, reversed, connectionState ->
            Triple(excludedIds, reversed, connectionState)
        },
    ) { segment, attempts, starPrompt, selectedId, (excludedIds, reversed, connectionState) ->
        latestSegmentExternalId = segment?.externalId

        // Lap numbering ("Ride 1", "Ride 2", ...) stays stable regardless of exclusion — it's
        // computed over every attempt, not just the ones currently shown in "All Attempts".
        val chronological = attempts.sortedBy { it.startTime }
        val lapLabels = lapLabelsByAttemptId(chronological)
        val (visible, excluded) = chronological.partition { it.id !in excludedIds }

        // Personal best, deltas, and the chart itself only ever consider visible (non-excluded)
        // attempts — an excluded attempt shouldn't be able to set the PR everything else is
        // measured against.
        val sortedByDuration = visible.sortedBy { it.duration }
        val personalBest = sortedByDuration.firstOrNull()
        val personalBestDeltaSeconds = if (sortedByDuration.size >= 2) {
            sortedByDuration[1].duration.seconds - sortedByDuration[0].duration.seconds
        } else {
            null
        }
        // 1st/2nd/3rd fastest non-excluded attempts — an excluded attempt can't hold any of these,
        // same reasoning as it can't hold the personal best.
        val rankByAttemptId = sortedByDuration.take(3).mapIndexed { index, attempt -> attempt.id to index + 1 }.toMap()

        val minSeconds = visible.minOfOrNull { it.duration.seconds }
        val maxSeconds = visible.maxOfOrNull { it.duration.seconds }
        val progressPoints = visible.map { attempt ->
            ProgressPoint(
                attemptId = attempt.id,
                normalizedY = normalizedSpeed(attempt.duration.seconds, minSeconds, maxSeconds),
                rank = rankByAttemptId[attempt.id],
            )
        }

        SegmentDetailUiState(
            isLoading = false,
            segment = segment,
            routePoints = segment?.routePoints().orEmpty(),
            personalBest = personalBest?.toItem(personalBest.duration.seconds, lapLabels.getValue(personalBest.id), rankByAttemptId[personalBest.id]),
            personalBestDeltaSeconds = personalBestDeltaSeconds,
            progressPoints = progressPoints,
            attempts = visible.map {
                it.toItem(personalBest?.duration?.seconds ?: 0, lapLabels.getValue(it.id), rankByAttemptId[it.id])
            }.let { if (reversed) it.asReversed() else it },
            excludedAttempts = excluded.map {
                it.toItem(personalBest?.duration?.seconds ?: 0, lapLabels.getValue(it.id), rankByAttemptId[it.id])
            }.let { if (reversed) it.asReversed() else it },
            starPrompt = starPrompt,
            selectedAttemptId = selectedId,
            attemptsReversed = reversed,
            stravaNotConnected = connectionState !is StravaConnectionState.Connected,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SegmentDetailUiState(),
    )

    fun onAttemptSelected(attemptId: Long) {
        selectedAttemptId.value = attemptId
    }

    /** Swiped out of "All Attempts" — hides it from the chart and moves it to the excluded section. */
    fun onAttemptExcluded(attemptId: Long) {
        viewModelScope.launch { setAttemptExcluded(attemptId, true) }
    }

    /** Swiped back in from the excluded section — restores it to "All Attempts" and the chart. */
    fun onAttemptIncluded(attemptId: Long) {
        viewModelScope.launch { setAttemptExcluded(attemptId, false) }
    }

    fun onToggleAttemptsOrder() {
        attemptsReversed.value = !attemptsReversed.value
    }

    fun onDismissStarPrompt() {
        starPromptState.value = null
    }

    fun onStarSegmentClick() {
        val externalId = latestSegmentExternalId ?: return
        starPromptState.value = StarPromptState(isSaving = true)
        viewModelScope.launch {
            setSegmentStarred(externalId, true).fold(
                onSuccess = { starPromptState.value = null },
                onFailure = { starPromptState.value = StarPromptState(isSaving = false) },
            )
        }
    }
}

/** Faster (lower seconds) plots higher on the chart — the intuitive "improving" reading. */
private fun normalizedSpeed(seconds: Long, min: Long?, max: Long?): Float {
    if (min == null || max == null || max == min) return 1f
    return 1f - (seconds - min).toFloat() / (max - min).toFloat()
}

private fun SegmentAttempt.toItem(personalBestSeconds: Long, lapLabel: String, rank: Int?): AttemptItem = AttemptItem(
    id = id,
    rideId = rideId,
    rideName = rideName,
    dateLabel = startTime.toRideCardDate(),
    lapLabel = lapLabel,
    durationLabel = duration.toRideClock(),
    deltaVsPrSeconds = duration.seconds - personalBestSeconds,
    rank = rank,
    isFromStrava = isFromStrava,
)
