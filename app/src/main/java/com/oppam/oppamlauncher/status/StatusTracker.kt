package com.oppam.oppamlauncher.status

import android.content.Context
import android.content.SharedPreferences

/**
 * Track online/offline status of Elder
 */
class StatusTracker(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "status_prefs",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_LAST_SEEN = "last_seen_timestamp"
        private const val KEY_IS_ONLINE = "is_online"
        private const val ONLINE_THRESHOLD_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    /**
     * Update that elder is currently active
     */
    fun updateElderOnline() {
        prefs.edit().apply {
            putLong(KEY_LAST_SEEN, System.currentTimeMillis())
            putBoolean(KEY_IS_ONLINE, true)
            apply()
        }
    }
    
    /**
     * Mark elder as offline
     */
    fun markOffline() {
        prefs.edit().putBoolean(KEY_IS_ONLINE, false).apply()
    }
    
    /**
     * Check if elder is currently online (based on last heartbeat)
     */
    fun isElderOnline(): Boolean {
        val lastSeen = prefs.getLong(KEY_LAST_SEEN, 0L)
        if (lastSeen == 0L) return false
        
        val timeSinceLastSeen = System.currentTimeMillis() - lastSeen
        return timeSinceLastSeen < ONLINE_THRESHOLD_MS
    }
    
    /**
     * Get last seen time in readable format
     */
    fun getLastSeenText(): String {
        val lastSeen = prefs.getLong(KEY_LAST_SEEN, 0L)
        if (lastSeen == 0L) return "Never"
        
        val minutesAgo = (System.currentTimeMillis() - lastSeen) / (60 * 1000)
        
        return when {
            minutesAgo < 1 -> "Just now"
            minutesAgo < 60 -> "$minutesAgo min ago"
            minutesAgo < 1440 -> "${minutesAgo / 60} hours ago"
            else -> "${minutesAgo / 1440} days ago"
        }
    }
}
