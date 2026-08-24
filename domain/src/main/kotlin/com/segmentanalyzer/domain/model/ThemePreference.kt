package com.segmentanalyzer.domain.model

/** The rider's chosen app theme, set from Settings. */
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    DRACULA,
    /** Sourced from the app icon on the About page: its stopwatch orange and sky blue on a deep navy. */
    TRAILHEAD,
}
