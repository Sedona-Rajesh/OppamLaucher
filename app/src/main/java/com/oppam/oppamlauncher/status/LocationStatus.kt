package com.oppam.oppamlauncher.status

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores last known location (no history by default).
 */
class LocationStatus(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("oppam_location", Context.MODE_PRIVATE)

    data class LastLocation(
        val lat: Double,
        val lng: Double,
        val accuracy: Float,
        val timestamp: Long
    )

    fun update(lat: Double, lng: Double, accuracy: Float, timestamp: Long) {
        prefs.edit()
            .putLong("timestamp", timestamp)
            .putString("lat", lat.toString())
            .putString("lng", lng.toString())
            .putFloat("accuracy", accuracy)
            .apply()
    }

    fun get(): LastLocation? {
        val ts = prefs.getLong("timestamp", 0L)
        val latStr = prefs.getString("lat", null)
        val lngStr = prefs.getString("lng", null)
        val acc = prefs.getFloat("accuracy", -1f)
        return if (ts > 0 && latStr != null && lngStr != null) {
            LastLocation(latStr.toDouble(), lngStr.toDouble(), acc, ts)
        } else null
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
