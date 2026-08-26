package com.segmentanalyzer.core.ui

import com.segmentanalyzer.domain.model.ActivityType

/** Short display name for an [ActivityType], e.g. "E-MTB". */
fun ActivityType.label(): String = when (this) {
    ActivityType.MTB -> "MTB"
    ActivityType.EMTB -> "E-MTB"
    ActivityType.GRAVEL -> "Gravel"
    ActivityType.EGRAVEL -> "E-Gravel"
    ActivityType.ROAD -> "Road"
    ActivityType.EROAD -> "E-Road"
    ActivityType.OTHER -> "Other"
}
