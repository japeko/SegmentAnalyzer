package com.segmentanalyzer.feature.analysis.compare

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.common.format.toRideCardDate
import com.segmentanalyzer.common.format.toRideClock
import com.segmentanalyzer.domain.model.GuestAttempt
import com.segmentanalyzer.domain.model.LatLng
import com.segmentanalyzer.domain.model.RideComparisonAttemptSummary
import com.segmentanalyzer.domain.model.RideComparisonGapPoint
import com.segmentanalyzer.domain.model.RideComparisonSummary
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.usecase.BuildSpeedSeriesUseCase
import com.segmentanalyzer.domain.usecase.BuildTimeGapSeriesUseCase
import com.segmentanalyzer.domain.usecase.GenerateRideComparisonInsightUseCase
import com.segmentanalyzer.domain.usecase.GetAttemptTrackUseCase
import com.segmentanalyzer.domain.usecase.GetGuestAttemptTrackUseCase
import com.segmentanalyzer.domain.usecase.ObserveGuestAttemptsForSegmentUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideComparisonInsightAvailabilityUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentAttemptsUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentsUseCase
import com.segmentanalyzer.domain.util.gradientPercentSegments
import com.segmentanalyzer.domain.util.lapLabelsByAttemptId
import com.segmentanalyzer.domain.util.routePoints
import com.segmentanalyzer.domain.util.slopeProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

private data class AddSheetState(val isVisible: Boolean = false, val selectedAddableId: Long? = null)

private data class PickerAndAiState(
    val sheet: AddSheetState,
    val referenceId: Long,
    val aiAvailable: Boolean,
    val aiInsight: AiInsightState,
    val guestAttempts: List<GuestAttempt>,
)

/**
 * A [SegmentAttempt] or a [GuestAttempt], flattened into the one shape the rest of this file
 * operates over — [id] is negative for a guest ("-guestAttempt.id"), since real attempt ids are
 * always positive (Room autoincrement starts at 1); this makes the two id spaces disjoint without
 * threading a sealed type through chips/series/stat-rows/the AI insight prompt.
 */
private data class ComparableAttempt(
    val id: Long,
    val startTime: Instant,
    val durationSeconds: Long,
    val avgSpeedKmh: Double,
    val avgPowerWatts: Double?,
    val isGuest: Boolean,
    val guestRiderName: String? = null,
)

private fun SegmentAttempt.toComparable() = ComparableAttempt(
    id = id,
    startTime = startTime,
    durationSeconds = duration.seconds,
    avgSpeedKmh = avgSpeedKmh,
    avgPowerWatts = avgPowerWatts,
    isGuest = false,
)

private fun GuestAttempt.toComparable() = ComparableAttempt(
    id = -id,
    startTime = startTime,
    durationSeconds = duration.seconds,
    avgSpeedKmh = avgSpeedKmh,
    avgPowerWatts = null,
    isGuest = true,
    guestRiderName = riderName,
)

@HiltViewModel
class RideCompareViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeSegments: ObserveSegmentsUseCase,
    observeSegmentAttempts: ObserveSegmentAttemptsUseCase,
    observeGuestAttempts: ObserveGuestAttemptsForSegmentUseCase,
    private val buildTimeGapSeries: BuildTimeGapSeriesUseCase,
    private val buildSpeedSeries: BuildSpeedSeriesUseCase,
    private val getAttemptTrack: GetAttemptTrackUseCase,
    private val getGuestAttemptTrack: GetGuestAttemptTrackUseCase,
    observeAiInsightAvailability: ObserveRideComparisonInsightAvailabilityUseCase,
    private val generateRideComparisonInsight: GenerateRideComparisonInsightUseCase,
) : ViewModel() {

    private val segmentId: Long = checkNotNull(savedStateHandle["segmentId"])

    /** The ride the screen was opened from — fixed for the life of the screen. Always the "Current" chip; unlike [referenceAttemptId], tapping another chip never changes this. */
    private val anchorAttemptId: Long = checkNotNull(savedStateHandle["anchorAttemptId"])

    /** Which attempt draws as the flat/zero-gap reference line on the Time Gap chart and feeds the route map. Starts at [anchorAttemptId], but the user can tap any chip — including a guest's — to switch; this never changes a chip's role label, only which one the others are measured against. */
    private val referenceAttemptId = MutableStateFlow(anchorAttemptId)

    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val excludedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val addSheetState = MutableStateFlow(AddSheetState())

    /** Whether this phone's on-device model is ready right now — re-checked on subscribe and reactively flips true if a background download finishes mid-session; see [ObserveRideComparisonInsightAvailabilityUseCase]. */
    private val isAiInsightAvailable = observeAiInsightAvailability()

    private val aiInsight = MutableStateFlow<AiInsightState>(AiInsightState.Idle)

    /** The comparison data an AI insight would be generated from, as of the most recent [uiState] emission — read (not observed) by [onGenerateInsightClick]. */
    private var latestComparisonSummary: RideComparisonSummary? = null

    val uiState: StateFlow<RideCompareUiState> = combine(
        observeSegments().map { segments -> segments.find { it.id == segmentId } },
        observeSegmentAttempts(segmentId),
        selectedIds,
        excludedIds,
        combine(
            addSheetState,
            referenceAttemptId,
            isAiInsightAvailable,
            aiInsight,
            observeGuestAttempts(segmentId),
        ) { sheet, referenceId, aiAvailable, aiInsightState, guestAttempts ->
            PickerAndAiState(sheet, referenceId, aiAvailable, aiInsightState, guestAttempts)
        },
    ) { segment, attempts, selected, excluded, (sheet, referenceId, aiAvailable, aiInsightState, guestAttempts) ->
        if (segment == null || attempts.isEmpty()) {
            return@combine RideCompareUiState(isLoading = true)
        }

        val comparable = attempts.map { it.toComparable() } + guestAttempts.map { it.toComparable() }
        val realOnly = comparable.filterNot { it.isGuest }

        val current = realOnly.find { it.id == anchorAttemptId }
        val personalBest = realOnly.minByOrNull { it.durationSeconds }
        val previous = realOnly
            .filter { it.id != anchorAttemptId && current != null && it.startTime.isBefore(current.startTime) }
            .maxByOrNull { it.startTime }

        fun roleFor(id: Long): AttemptRole = when {
            id < 0 -> AttemptRole.GUEST
            id == current?.id -> AttemptRole.CURRENT
            id == personalBest?.id -> AttemptRole.PERSONAL_BEST
            id == previous?.id -> AttemptRole.PREVIOUS
            else -> AttemptRole.SELECTED
        }

        // Current is always shown — it's the screen's anchor. Personal Best/Previous are only
        // defaults; the user can dismiss either one (onRemoveAttempt), and re-add it later via
        // the picker if they want it back. A guest is never a default — always opt-in via the
        // Add sheet's "Guest Rides" section.
        val defaultIds = listOfNotNull(
            current?.id,
            personalBest?.id?.takeIf { it != current?.id && it !in excluded },
            previous?.id?.takeIf { it != current?.id && it != personalBest?.id && it !in excluded },
        )
        val orderedIds = (defaultIds + selected.filter { it !in defaultIds }).distinct()
        val lapLabels = lapLabelsByAttemptId(attempts)

        val chips = orderedIds.mapIndexedNotNull { index, id ->
            comparable.find { it.id == id }?.let { attempt ->
                AttemptChip(
                    attemptId = id,
                    role = roleFor(id),
                    isReference = id == referenceId,
                    dateLabel = attempt.startTime.toRideCardDate(),
                    lapLabel = if (attempt.isGuest) checkNotNull(attempt.guestRiderName) else lapLabels.getValue(id),
                    durationLabel = attempt.durationSeconds.toDurationLabel(),
                    colorIndex = index,
                )
            }
        }

        // Fetched per-chip (not just the reference) since Time Gap/Speed series need every
        // visible chip's track too — a guest's comes from a different table entirely, dispatched
        // here by id sign so BuildTimeGapSeriesUseCase/BuildSpeedSeriesUseCase don't need to know
        // either table exists; they just take tracks.
        val tracksByChipId: Map<Long, List<TrackPoint>> = chips.associate { chip ->
            chip.attemptId to if (chip.attemptId < 0) getGuestAttemptTrack(-chip.attemptId) else getAttemptTrack(chip.attemptId)
        }
        val track = tracksByChipId[referenceId].orEmpty()

        val otherIds = chips.map { it.attemptId }.filter { it != referenceId }
        val referenceChipExists = chips.any { it.attemptId == referenceId }
        val timeGapSeries = if (referenceChipExists && otherIds.isNotEmpty()) {
            val colorByAttemptId = chips.associate { it.attemptId to it.colorIndex }
            buildTimeGapSeries(track, otherIds.associateWith { tracksByChipId.getValue(it) }, segment.distanceMeters).map { series ->
                TimeGapSeriesUi(
                    attemptId = series.attemptId,
                    colorIndex = colorByAttemptId[series.attemptId] ?: 0,
                    points = series.points,
                )
            }
        } else {
            emptyList()
        }

        val allChipIds = chips.map { it.attemptId }
        val speedSeries = if (allChipIds.isNotEmpty()) {
            val colorByAttemptId = chips.associate { it.attemptId to it.colorIndex }
            buildSpeedSeries(allChipIds.associateWith { tracksByChipId.getValue(it) }, segment.distanceMeters).map { series ->
                SpeedSeriesUi(
                    attemptId = series.attemptId,
                    colorIndex = colorByAttemptId[series.attemptId] ?: 0,
                    points = series.points,
                )
            }
        } else {
            emptyList()
        }

        val slopePoints = slopeProfile(track)

        latestComparisonSummary = buildComparisonSummary(segment.name, segment.distanceMeters, chips, comparable, timeGapSeries)

        val addableOwn = attempts.map { attempt ->
            AddableAttemptItem(
                id = attempt.id,
                dateLabel = attempt.startTime.toRideCardDate(),
                lapLabel = lapLabels.getValue(attempt.id),
                statsLabel = "${attempt.duration.toRideClock()} · %.1f km/h".format(attempt.avgSpeedKmh),
                statusLabel = when {
                    attempt.id == anchorAttemptId -> "CURRENT · ${lapLabels.getValue(attempt.id)}"
                    attempt.id in orderedIds -> "ADDED · ${lapLabels.getValue(attempt.id)}"
                    else -> null
                },
            )
        }
        val addableGuests = guestAttempts.map { guest ->
            val chipId = -guest.id
            AddableAttemptItem(
                id = chipId,
                dateLabel = guest.startTime.toRideCardDate(),
                lapLabel = guest.riderName,
                statsLabel = "${guest.duration.toRideClock()} · %.1f km/h".format(guest.avgSpeedKmh),
                statusLabel = if (chipId in orderedIds) "ADDED · ${guest.riderName}" else null,
                isGuest = true,
            )
        }

        RideCompareUiState(
            isLoading = false,
            segmentName = segment.name,
            routePoints = if (track.isNotEmpty()) track.map { LatLng(it.latitude, it.longitude) } else segment.routePoints(),
            gradientPercents = if (track.isNotEmpty()) gradientPercentSegments(track) else null,
            chips = chips,
            timeGapSeries = timeGapSeries,
            speedSeries = speedSeries,
            slopePoints = slopePoints,
            segmentDistanceMeters = segment.distanceMeters,
            statRows = buildStatRows(chips, comparable),
            isAddSheetVisible = sheet.isVisible,
            addableAttempts = addableOwn + addableGuests,
            selectedAddableId = sheet.selectedAddableId,
            isAiInsightAvailable = aiAvailable,
            aiInsight = aiInsightState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RideCompareUiState(),
    )

    fun onAddClick() {
        addSheetState.value = AddSheetState(isVisible = true)
    }

    fun onDismissAddSheet() {
        addSheetState.value = AddSheetState()
    }

    fun onAddableAttemptSelected(attemptId: Long) {
        addSheetState.value = addSheetState.value.copy(selectedAddableId = attemptId)
    }

    fun onConfirmAdd() {
        addSheetState.value.selectedAddableId?.let { id -> selectedIds.value = selectedIds.value + id }
        addSheetState.value = AddSheetState()
    }

    /**
     * Removes a chip from the comparison — a manually-added one (own or guest), or a Personal
     * Best/Previous default. A no-op for the anchor or the current reference: the anchor is the
     * screen's identity and the UI never offers it; the reference is the chart/map's baseline,
     * and removing its chip while it's still driving them would leave it visually gone but still
     * affecting what's shown. Excluding a default's id keeps it from being auto-picked again;
     * re-adding it via the picker still works, since [selectedIds] is checked independently of
     * [excludedIds].
     */
    fun onRemoveAttempt(attemptId: Long) {
        if (attemptId == anchorAttemptId || attemptId == referenceAttemptId.value) return
        selectedIds.value = selectedIds.value - attemptId
        excludedIds.value = excludedIds.value + attemptId
    }

    /**
     * Makes [attemptId] the new flat-line reference for the Time Gap chart and route map — shown
     * via [AttemptChip.isReference], not by changing anyone's role label (Personal Best stays
     * Personal Best even while it's the reference; only the true anchor ride is ever "Current").
     * Works the same for a guest chip (a negative id) as for the rider's own.
     */
    fun onSetReferenceClick(attemptId: Long) {
        referenceAttemptId.value = attemptId
    }

    /** Generates an on-device AI explanation of the current comparison. A no-op if the comparison hasn't loaded yet — the UI never offers this action before then. */
    fun onGenerateInsightClick() {
        val summary = latestComparisonSummary ?: return
        aiInsight.value = AiInsightState.Loading
        viewModelScope.launch {
            aiInsight.value = generateRideComparisonInsight(summary).fold(
                onSuccess = { text -> AiInsightState.Loaded(text) },
                onFailure = { throwable -> AiInsightState.Error(throwable.message ?: "Couldn't generate an insight.") },
            )
        }
    }
}

/** "15:31" from a raw seconds count — same formatting as [java.time.Duration.toRideClock] without needing to wrap/unwrap a Duration for every [ComparableAttempt]. */
private fun Long.toDurationLabel(): String = java.time.Duration.ofSeconds(this).toRideClock()

/**
 * Always exactly two rides — the reference plus its closest rival (the fastest of the others,
 * guest or not) — even when more chips are visible in the comparison. More than two muddies both
 * "who was fastest" and "where did the reference lose/gain time" into a three-or-more-way
 * comparison the model has no reliable way to keep straight, and isn't what "compare this ride to
 * my PR" (or to a friend's ride) means anyway when Previous or another manually-added ride also
 * happens to be on screen.
 */
private fun buildComparisonSummary(
    segmentName: String,
    segmentDistanceMeters: Double,
    chips: List<AttemptChip>,
    attempts: List<ComparableAttempt>,
    timeGapSeries: List<TimeGapSeriesUi>,
): RideComparisonSummary {
    val referenceChip = chips.find { it.isReference }
    val referenceAttempt = referenceChip?.let { chip -> attempts.find { it.id == chip.attemptId } }
    val rivalChip = chips
        .filter { !it.isReference }
        .mapNotNull { chip -> attempts.find { it.id == chip.attemptId }?.let { chip to it } }
        .minByOrNull { (_, attempt) -> attempt.durationSeconds }
        ?.first
    val comparedChips = listOfNotNull(referenceChip, rivalChip)

    return RideComparisonSummary(
        segmentName = segmentName,
        segmentDistanceMeters = segmentDistanceMeters,
        referenceLabel = referenceChip?.let { chip -> labelFor(chip, referenceAttempt) } ?: "the reference ride",
        attempts = comparedChips.mapNotNull { chip ->
            val attempt = attempts.find { it.id == chip.attemptId } ?: return@mapNotNull null
            val series = timeGapSeries.find { it.attemptId == chip.attemptId }
            RideComparisonAttemptSummary(
                label = labelFor(chip, attempt),
                durationSeconds = attempt.durationSeconds,
                avgSpeedKmh = attempt.avgSpeedKmh,
                avgPowerWatts = attempt.avgPowerWatts,
                finalGapSeconds = series?.points?.lastOrNull()?.gapSeconds,
                // Tracked separately (not just "the single biggest swing") so an early loss that's
                // partly clawed back later doesn't get silently dropped in favor of a bigger — but
                // less rider-relevant — moment of being ahead elsewhere.
                worstPoint = series?.points?.filter { it.gapSeconds > 0 }?.maxByOrNull { it.gapSeconds }
                    ?.let { RideComparisonGapPoint(it.distanceMeters, it.gapSeconds) },
                bestPoint = series?.points?.filter { it.gapSeconds < 0 }?.minByOrNull { it.gapSeconds }
                    ?.let { RideComparisonGapPoint(it.distanceMeters, it.gapSeconds) },
                isGuest = attempt.isGuest,
            )
        },
    )
}

/** A guest is identified by the rider's own name rather than a generic role label — much more useful in an AI-generated sentence than "Guest". */
private fun labelFor(chip: AttemptChip, attempt: ComparableAttempt?): String =
    if (attempt?.isGuest == true) "${attempt.guestRiderName} (${chip.dateLabel})" else "${chip.role.promptLabel()} (${chip.dateLabel})"

private fun AttemptRole.promptLabel(): String = when (this) {
    AttemptRole.CURRENT -> "Current ride"
    AttemptRole.PERSONAL_BEST -> "Personal Best"
    AttemptRole.PREVIOUS -> "Previous ride"
    AttemptRole.SELECTED -> "Selected ride"
    AttemptRole.GUEST -> "Guest ride"
}

private fun buildStatRows(chips: List<AttemptChip>, attempts: List<ComparableAttempt>): List<CompareStatRow> {
    val chipAttempts = chips.mapNotNull { chip -> attempts.find { it.id == chip.attemptId }?.let { chip to it } }
    if (chipAttempts.isEmpty()) return emptyList()

    val rows = mutableListOf<CompareStatRow>()

    val maxDuration = chipAttempts.maxOf { it.second.durationSeconds }.toDouble()
    val minDuration = chipAttempts.minOf { it.second.durationSeconds }
    rows += CompareStatRow(
        label = "Total Time",
        values = chipAttempts.map { (chip, attempt) ->
            CompareStatValue(
                attemptId = chip.attemptId,
                colorIndex = chip.colorIndex,
                label = attempt.durationSeconds.toDurationLabel(),
                fraction = fraction(attempt.durationSeconds.toDouble(), maxDuration),
                isBest = attempt.durationSeconds == minDuration,
            )
        },
    )

    val maxSpeed = chipAttempts.maxOf { it.second.avgSpeedKmh }
    rows += CompareStatRow(
        label = "Avg Speed",
        values = chipAttempts.map { (chip, attempt) ->
            CompareStatValue(
                attemptId = chip.attemptId,
                colorIndex = chip.colorIndex,
                label = "%.1f km/h".format(attempt.avgSpeedKmh),
                fraction = fraction(attempt.avgSpeedKmh, maxSpeed),
                isBest = attempt.avgSpeedKmh == maxSpeed,
            )
        },
    )

    val powers = chipAttempts.mapNotNull { it.second.avgPowerWatts }
    if (powers.isNotEmpty()) {
        val maxPower = powers.max()
        rows += CompareStatRow(
            label = "Avg Power",
            values = chipAttempts.mapNotNull { (chip, attempt) ->
                attempt.avgPowerWatts?.let { power ->
                    CompareStatValue(
                        attemptId = chip.attemptId,
                        colorIndex = chip.colorIndex,
                        label = "%.0f W".format(power),
                        fraction = fraction(power, maxPower),
                        isBest = power == maxPower,
                    )
                }
            },
        )
    }

    return rows
}

private fun fraction(value: Double, max: Double): Float = if (max <= 0.0) 0f else (value / max).toFloat()
