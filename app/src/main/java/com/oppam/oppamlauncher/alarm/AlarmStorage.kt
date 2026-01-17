package com.oppam.oppamlauncher.alarm

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Store scheduled alarms
 */
class AlarmStorage(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "scheduled_alarms",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_ALARMS = "alarms"
    }
    
    data class ScheduledAlarm(
        val id: Int,
        val message: String,
        val timeInMillis: Long,
        val repeatDaily: Boolean = false,
        val status: String = "scheduled", // scheduled, dismissed, snoozed, missed
        val dismissedAt: Long = 0L
    )
    
    /**
     * Save a scheduled alarm
     */
    fun saveAlarm(alarm: ScheduledAlarm) {
        val alarms = getAllAlarms().toMutableList()
        alarms.removeAll { it.id == alarm.id } // Remove if exists
        alarms.add(alarm)
        saveAllAlarms(alarms)
    }
    
    /**
     * Get all scheduled alarms
     */
    fun getAllAlarms(): List<ScheduledAlarm> {
        val json = prefs.getString(KEY_ALARMS, "[]") ?: "[]"
        val jsonArray = JSONArray(json)
        val alarms = mutableListOf<ScheduledAlarm>()
        
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            alarms.add(
                ScheduledAlarm(
                    id = obj.getInt("id"),
                    message = obj.getString("message"),
                    timeInMillis = obj.getLong("timeInMillis"),
                    repeatDaily = obj.optBoolean("repeatDaily", false),
                    status = obj.optString("status", "scheduled"),
                    dismissedAt = obj.optLong("dismissedAt", 0L)
                )
            )
        }
        
        return alarms
    }
    
    /**
     * Save all alarms
     */
    private fun saveAllAlarms(alarms: List<ScheduledAlarm>) {
        val jsonArray = JSONArray()
        
        alarms.forEach { alarm ->
            val obj = JSONObject().apply {
                put("id", alarm.id)
                put("message", alarm.message)
                put("timeInMillis", alarm.timeInMillis)
                put("repeatDaily", alarm.repeatDaily)
                put("status", alarm.status)
                put("dismissedAt", alarm.dismissedAt)
            }
            jsonArray.put(obj)
        }
        
        prefs.edit().putString(KEY_ALARMS, jsonArray.toString()).apply()
    }
    
    /**
     * Delete an alarm
     */
    fun deleteAlarm(alarmId: Int) {
        val alarms = getAllAlarms().filter { it.id != alarmId }
        saveAllAlarms(alarms)
    }
    
    /**
     * Generate unique alarm ID
     */
    fun generateAlarmId(): Int {
        val maxId = getAllAlarms().maxOfOrNull { it.id } ?: 0
        return maxId + 1
    }
    
    /**
     * Update alarm status
     */
    fun updateAlarmStatus(alarmId: Int, newStatus: String, dismissedAt: Long = System.currentTimeMillis()) {
        val alarms = getAllAlarms().map { alarm ->
            if (alarm.id == alarmId) {
                alarm.copy(status = newStatus, dismissedAt = dismissedAt)
            } else {
                alarm
            }
        }
        saveAllAlarms(alarms)
    }
    
    /**
     * Get alarms by status
     */
    fun getAlarmsByStatus(status: String): List<ScheduledAlarm> {
        return getAllAlarms().filter { it.status == status }
    }
    
    /**
     * Get upcoming scheduled alarms
     */
    fun getUpcomingAlarms(): List<ScheduledAlarm> {
        val now = System.currentTimeMillis()
        return getAllAlarms()
            .filter { it.status == "scheduled" && it.timeInMillis > now }
            .sortedBy { it.timeInMillis }
    }
    
    /**
     * Mark missed alarms (past scheduled time)
     */
    fun markMissedAlarms() {
        val now = System.currentTimeMillis()
        val alarms = getAllAlarms().map { alarm ->
            if (alarm.status == "scheduled" && alarm.timeInMillis < now) {
                alarm.copy(status = "missed")
            } else {
                alarm
            }
        }
        saveAllAlarms(alarms)
    }
}
