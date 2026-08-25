package com.segmentanalyzer.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** On-device storage for which ride ids the rider has opened the detail screen for. */
@Singleton
internal class ViewedRidesStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _viewedRideIds = MutableStateFlow(readViewedRideIds())
    val viewedRideIds: StateFlow<Set<Long>> = _viewedRideIds.asStateFlow()

    fun markViewed(rideId: Long) {
        val updated = _viewedRideIds.value + rideId
        prefs.edit().putStringSet(KEY_VIEWED_IDS, updated.map { it.toString() }.toSet()).apply()
        _viewedRideIds.value = updated
    }

    private fun readViewedRideIds(): Set<Long> =
        prefs.getStringSet(KEY_VIEWED_IDS, null)?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()

    private companion object {
        const val PREFS_NAME = "viewed_rides"
        const val KEY_VIEWED_IDS = "viewed_ride_ids"
    }
}
