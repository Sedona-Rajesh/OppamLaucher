package com.oppam.oppamlauncher.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.oppam.oppamlauncher.companion.AIHealthCompanion
import com.oppam.oppamlauncher.reminders.ReminderPreferences
import com.oppam.oppamlauncher.reminders.ReminderSystem
import kotlinx.coroutines.*

/**
 * Background service for AI companion and reminders
 */
class CompanionService : Service() {
    
    private var aiCompanion: AIHealthCompanion? = null
    private var reminderSystem: ReminderSystem? = null
    private var serviceJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize AI companion
        aiCompanion = AIHealthCompanion(this)
        aiCompanion?.startCompanion()
        
        // Initialize reminder system
        reminderSystem = ReminderSystem(this)
        
        // Start monitoring for reminders
        startReminderMonitoring()
    }
    
    private fun startReminderMonitoring() {
        serviceJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                checkAndShowReminders()
                delay(10_000) // Check every 10 seconds
            }
        }
    }
    
    private fun checkAndShowReminders() {
        val reminderPrefs = ReminderPreferences(this)
        val activeReminders = reminderPrefs.getActiveReminders()
        
        activeReminders.forEach { reminder ->
            reminderSystem?.showReminder(reminder)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
        aiCompanion?.cleanup()
        reminderSystem?.cleanup()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
