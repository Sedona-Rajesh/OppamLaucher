package com.oppam.oppamlauncher.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.AlarmManager.AlarmClockInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

/**
 * Schedule alarms on Elder's phone
 */
object AlarmScheduler {
    
    private const val TAG = "AlarmScheduler"
    
    /**
     * Schedule an alarm to ring at specific time
     */
    fun scheduleAlarm(context: Context, alarmId: Int, message: String, timeInMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // PendingIntent that will run when the alarm actually fires
        val triggerIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("MESSAGE", message)
            putExtra("SCHEDULED_TIME", timeInMillis) // Pass scheduled time for logging
        }
        val triggerPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            triggerIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Activity intent used for user-visible alarm affordance (guarantees background start)
        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("MESSAGE", message)
            putExtra("SCHEDULED_TIME", timeInMillis)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarmId,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        val alarmTime = dateFormat.format(Date(timeInMillis))
        
        Log.d(TAG, "Scheduling alarm ID=$alarmId for $alarmTime. Message: $message")
        
        // Prefer AlarmClockInfo to bypass background-start restrictions reliably
        try {
            val alarmClockInfo = AlarmClockInfo(timeInMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, triggerPendingIntent)
            Log.d(TAG, "Alarm scheduled using setAlarmClock (reliable background start)")
        } catch (e: Exception) {
            Log.e(TAG, "setAlarmClock failed, falling back", e)
            // Fallback scheduling paths
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        triggerPendingIntent
                    )
                    Log.d(TAG, "Alarm scheduled using setExactAndAllowWhileIdle (fallback)")
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        triggerPendingIntent
                    )
                    Log.d(TAG, "Alarm scheduled using setAndAllowWhileIdle (inexact fallback)")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    triggerPendingIntent
                )
                Log.d(TAG, "Alarm scheduled using setExactAndAllowWhileIdle (pre-S fallback)")
            }
        }
        
        Toast.makeText(context, "Alarm set for $alarmTime", Toast.LENGTH_LONG).show()
        Log.d(TAG, "Alarm scheduling complete.")
    }
    
    /**
     * Cancel a scheduled alarm
     */
    fun cancelAlarm(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
    
    /**
     * Schedule follow-up notification (5 mins after dismissal)
     */
    fun scheduleFollowUp(context: Context, alarmId: Int, message: String, timeInMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, FollowUpReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("MESSAGE", message)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId + 10000, // Different ID for follow-up
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Schedule follow-up notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }
}
