package com.oppam.oppamlauncher.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives BOOT_COMPLETED broadcast and re-schedules all pending alarms
 * This ensures alarms survive device reboots
 */
class BootReceiver : BroadcastReceiver() {
    
    private val TAG = "BootReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "========== BOOT COMPLETED ==========")
            Log.d(TAG, "Device rebooted - re-scheduling all pending alarms")
            
            val alarmStorage = AlarmStorage(context)
            val allAlarms = alarmStorage.getAllAlarms()
            
            val pendingAlarms = allAlarms.filter { alarm ->
                alarm.status == "scheduled" && alarm.timeInMillis > System.currentTimeMillis()
            }
            
            Log.d(TAG, "Found ${pendingAlarms.size} pending alarms to re-schedule")
            
            var rescheduledCount = 0
            pendingAlarms.forEach { alarm ->
                try {
                    AlarmScheduler.scheduleAlarm(
                        context = context,
                        alarmId = alarm.id,
                        message = alarm.message,
                        timeInMillis = alarm.timeInMillis
                    )
                    rescheduledCount++
                    Log.d(TAG, "Re-scheduled alarm ID=${alarm.id}, Message=${alarm.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to re-schedule alarm ID=${alarm.id}", e)
                }
            }
            
            Log.d(TAG, "Successfully re-scheduled $rescheduledCount out of ${pendingAlarms.size} alarms")
            Log.d(TAG, "===================================")
        }
    }
}
