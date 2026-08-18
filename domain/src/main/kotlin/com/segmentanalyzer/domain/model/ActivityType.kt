package com.segmentanalyzer.domain.model

/** The kind of riding a ride was, as classified at import time. */
enum class ActivityType {
    MTB,
    GRAVEL,
    ROAD,

    /** Fallback for activity types Segment Analyzer doesn't yet have a dedicated category for. */
    OTHER,
}
