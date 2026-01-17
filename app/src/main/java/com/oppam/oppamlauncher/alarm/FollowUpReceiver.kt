package com.oppam.oppamlauncher.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.oppam.oppamlauncher.MainActivity
import com.oppam.oppamlauncher.R
import com.oppam.oppamlauncher.auth.UserPreferences

/**
 * Receiver for 5-minute follow-up notifications after alarm dismissal
 */
class FollowUpReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("MESSAGE") ?: "മരുന്ന് കഴിച്ചോ?"
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        
        // Show follow-up notification
        showFollowUpNotification(context, message, alarmId)
    }
    
    private fun showFollowUpNotification(context: Context, message: String, alarmId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel (for Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "follow_up_channel",
                "Follow-up Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Follow-up reminders after alarm dismissal"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Intent to open app
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            alarmId,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Action for "Yes"
        val yesIntent = Intent(context, FollowUpActionReceiver::class.java).apply {
            action = "com.oppam.oppamlauncher.YES_ACTION"
            putExtra("ALARM_ID", alarmId)
        }
        val yesPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId + 30000,
            yesIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Action for "No"
        val noIntent = Intent(context, FollowUpActionReceiver::class.java).apply {
            action = "com.oppam.oppamlauncher.NO_ACTION"
            putExtra("ALARM_ID", alarmId)
            putExtra("MESSAGE", message)
        }
        val noPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId + 40000,
            noIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(context, "follow_up_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("അപ്പച്ചാ!")
            .setContentText("$message - കഴിച്ചോ?")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message - കഴിച്ചോ?"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .addAction(R.drawable.ic_launcher_foreground, "അതെ (Yes)", yesPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "ഇല്ല (No)", noPendingIntent)
            .build()
        
        notificationManager.notify(alarmId + 20000, notification)
    }
}
