package com.oppam.oppamlauncher.reminders

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Storage for reminders
 */
class ReminderPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "reminders_prefs",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_REMINDERS = "reminders"
        private const val KEY_SHOWN_REMINDERS = "shown_reminders"
    }
    
    /**
     * Save a reminder
     */
    fun saveReminder(reminder: Reminder) {
        val reminders = getAllReminders().toMutableList()
        reminders.add(reminder)
        saveAllReminders(reminders)
    }
    
    /**
     * Get all reminders
     */
    fun getAllReminders(): List<Reminder> {
        val json = prefs.getString(KEY_REMINDERS, "[]") ?: "[]"
        val jsonArray = JSONArray(json)
        val reminders = mutableListOf<Reminder>()
        
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            reminders.add(
                Reminder(
                    id = obj.getInt("id"),
                    titleMl = obj.getString("titleMl"),
                    titleEn = obj.getString("titleEn"),
                    messageMl = obj.getString("messageMl"),
                    messageEn = obj.getString("messageEn"),
                    time = obj.getLong("time"),
                    repeatType = obj.optString("repeatType", "once"),
                    isActive = obj.optBoolean("isActive", true),
                    fromCaregiver = obj.optString("fromCaregiver", "")
                )
            )
        }
        
        return reminders
    }
    
    /**
     * Get active reminders that need to be shown
     */
    fun getActiveReminders(): List<Reminder> {
        val currentTime = System.currentTimeMillis()
        val shownIds = getShownReminderIds()
        
        return getAllReminders().filter { reminder ->
            reminder.isActive &&
            reminder.time <= currentTime &&
            !shownIds.contains(reminder.id)
        }
    }
    
    /**
     * Save all reminders
     */
    private fun saveAllReminders(reminders: List<Reminder>) {
        val jsonArray = JSONArray()
        
        reminders.forEach { reminder ->
            val obj = JSONObject().apply {
                put("id", reminder.id)
                put("titleMl", reminder.titleMl)
                put("titleEn", reminder.titleEn)
                put("messageMl", reminder.messageMl)
                put("messageEn", reminder.messageEn)
                put("time", reminder.time)
                put("repeatType", reminder.repeatType)
                put("isActive", reminder.isActive)
                put("fromCaregiver", reminder.fromCaregiver)
            }
            jsonArray.put(obj)
        }
        
        prefs.edit().putString(KEY_REMINDERS, jsonArray.toString()).apply()
    }
    
    /**
     * Mark reminder as shown
     */
    fun markReminderShown(reminderId: Int) {
        val shown = getShownReminderIds().toMutableSet()
        shown.add(reminderId)
        prefs.edit().putStringSet(KEY_SHOWN_REMINDERS, shown.map { it.toString() }.toSet()).apply()
    }
    
    private fun getShownReminderIds(): Set<Int> {
        val stringSet = prefs.getStringSet(KEY_SHOWN_REMINDERS, emptySet()) ?: emptySet()
        return stringSet.mapNotNull { it.toIntOrNull() }.toSet()
    }
    
    /**
     * Delete a reminder
     */
    fun deleteReminder(reminderId: Int) {
        val reminders = getAllReminders().filter { it.id != reminderId }
        saveAllReminders(reminders)
    }
    
    /**
     * Generate unique reminder ID
     */
    fun generateReminderId(): Int {
        val maxId = getAllReminders().maxOfOrNull { it.id } ?: 0
        return maxId + 1
    }
}
