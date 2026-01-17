package com.oppam.oppamlauncher.companion

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

/**
 * Preferences for AI Companion
 */
class CompanionPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "companion_prefs",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_LAST_CHECK_IN = "last_check_in"
        private const val KEY_CHECK_IN_MORNING = "check_in_morning"
        private const val KEY_CHECK_IN_AFTERNOON = "check_in_afternoon"
        private const val KEY_CHECK_IN_EVENING = "check_in_evening"
        private const val KEY_HEALTH_LOG = "health_log"
    }
    
    fun getLastCheckInTime(): Long {
        return prefs.getLong(KEY_LAST_CHECK_IN, 0)
    }
    
    fun hasCheckedInToday(type: String): Boolean {
        val key = when (type) {
            "morning" -> KEY_CHECK_IN_MORNING
            "afternoon" -> KEY_CHECK_IN_AFTERNOON
            "evening" -> KEY_CHECK_IN_EVENING
            else -> return false
        }
        
        val lastCheckIn = prefs.getLong(key, 0)
        val today = getTodayTimestamp()
        
        return lastCheckIn >= today
    }
    
    fun markCheckInDone(type: String) {
        val key = when (type) {
            "morning" -> KEY_CHECK_IN_MORNING
            "afternoon" -> KEY_CHECK_IN_AFTERNOON
            "evening" -> KEY_CHECK_IN_EVENING
            else -> KEY_LAST_CHECK_IN
        }
        
        prefs.edit().putLong(key, System.currentTimeMillis()).apply()
        prefs.edit().putLong(KEY_LAST_CHECK_IN, System.currentTimeMillis()).apply()
    }
    
    private fun getTodayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    fun logCheckIn(type: String, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val log = "$timestamp [$type]: $message"
        
        val currentLog = prefs.getString(KEY_HEALTH_LOG, "") ?: ""
        val newLog = "$log\n$currentLog"
        
        // Keep only last 100 entries
        val lines = newLog.split("\n").take(100)
        prefs.edit().putString(KEY_HEALTH_LOG, lines.joinToString("\n")).apply()
    }
    
    fun logHealthData(type: String, value: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val log = "$timestamp [HEALTH-$type]: $value"
        
        val currentLog = prefs.getString(KEY_HEALTH_LOG, "") ?: ""
        val newLog = "$log\n$currentLog"
        
        val lines = newLog.split("\n").take(100)
        prefs.edit().putString(KEY_HEALTH_LOG, lines.joinToString("\n")).apply()
    }
    
    fun getHealthLog(): String {
        return prefs.getString(KEY_HEALTH_LOG, "") ?: ""
    }
}
