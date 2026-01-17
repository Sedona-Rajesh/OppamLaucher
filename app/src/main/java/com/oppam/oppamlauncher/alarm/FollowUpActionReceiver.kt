package com.oppam.oppamlauncher.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oppam.oppamlauncher.auth.UserPreferences

/**
 * Handles actions from the follow-up notification (Yes/No)
 */
class FollowUpActionReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val notificationId = alarmId + 20000
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        when (intent.action) {
            "com.oppam.oppamlauncher.YES_ACTION" -> {
                // User confirmed, just cancel the notification
                notificationManager.cancel(notificationId)
                
                // SMS status update disabled per requirement to avoid unwanted messages
            }
            "com.oppam.oppamlauncher.NO_ACTION" -> {
                // User did not confirm, re-schedule the follow-up
                notificationManager.cancel(notificationId)
                
                val message = intent.getStringExtra("MESSAGE") ?: "മരുന്ന് കഴിച്ചോ?"
                val followUpTime = System.currentTimeMillis() + (5 * 60 * 1000) // 5 more minutes
                
                AlarmScheduler.scheduleFollowUp(
                    context = context,
                    alarmId = alarmId,
                    message = message,
                    timeInMillis = followUpTime
                )
            }
        }
    }
}
