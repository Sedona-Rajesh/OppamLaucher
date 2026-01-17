package com.oppam.oppamlauncher.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives alarm triggers and launches alarm activity
 */
class AlarmReceiver : BroadcastReceiver() {
    
    private val TAG = "AlarmReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val message = intent.getStringExtra("MESSAGE") ?: "അപ്പച്ചാ!"
        val scheduledTime = intent.getLongExtra("SCHEDULED_TIME", 0)
        
        Log.d(TAG, "==================== ALARM TRIGGERED ====================")
        Log.d(TAG, "Alarm ID: $alarmId")
        Log.d(TAG, "Message: $message")
        Log.d(TAG, "Scheduled time: $scheduledTime")
        Log.d(TAG, "Current time: ${System.currentTimeMillis()}")
        
        // Launch full-screen alarm activity
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("MESSAGE", message)
            putExtra("ALARM_ID", alarmId)
            putExtra("SCHEDULED_TIME", scheduledTime)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        Log.d(TAG, "Launching AlarmActivity...")
        context.startActivity(alarmIntent)
        
        // Mark as triggered initially (will be updated based on user response)
        if (alarmId != -1) {
            val alarmStorage = AlarmStorage(context)
            val alarm = alarmStorage.getAllAlarms().find { it.id == alarmId }
            if (alarm?.status == "scheduled") {
                alarmStorage.updateAlarmStatus(alarmId, "triggered")
                Log.d(TAG, "Alarm marked as triggered (will update based on user response)")
            }
        }
        
        Log.d(TAG, "========================================================")
    }
}
