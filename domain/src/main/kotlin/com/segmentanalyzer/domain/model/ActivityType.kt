package com.segmentanalyzer.domain.model

/**
 * The kind of riding a ride was — classified at import time, but the rider can always correct it
 * (or set the e-bike variant, which nothing currently auto-detects) via Ride Detail's edit dialog.
 */
enum class ActivityType {
    MTB,
    EMTB,
    GRAVEL,
    EGRAVEL,
    ROAD,
    EROAD,

    /** Fallback for activity types Segment Analyzer doesn't yet have a dedicated category for. */
    OTHER,
}
