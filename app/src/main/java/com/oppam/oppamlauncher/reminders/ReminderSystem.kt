package com.oppam.oppamlauncher.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.oppam.oppamlauncher.MainActivity
import com.oppam.oppamlauncher.R
import java.util.*

/**
 * Reminder System for caregiver-set alerts
 */
class ReminderSystem(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var tts: TextToSpeech? = null
    
    companion object {
        private const val CHANNEL_ID = "oppam_reminders"
        private const val CHANNEL_NAME = "Oppam Reminders"
    }
    
    init {
        createNotificationChannel()
        initializeTTS()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders from caregiver"
                enableVibration(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    null
                )
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale("ml", "IN")
                if (tts?.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                    tts?.language = locale
                }
            }
        }
    }
    
    /**
     * Show reminder with notification and voice
     */
    fun showReminder(reminder: Reminder) {
        // Create notification
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(reminder.titleMl)
            .setContentText(reminder.messageMl)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()
        
        notificationManager.notify(reminder.id, notification)
        
        // Speak the reminder
        speakReminder(reminder.messageMl)
        
        // Mark as shown
        val prefs = ReminderPreferences(context)
        prefs.markReminderShown(reminder.id)
    }
    
    /**
     * Speak reminder message
     */
    fun speakReminder(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    /**
     * Speak incoming caregiver message
     */
    fun speakCaregiverMessage(senderName: String, message: String) {
        val announcement = "$senderName പറയുന്നു: $message"
        tts?.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    /**
     * Show alarm notification with AI voice when caregiver sends reminder via SMS
     */
    fun showAlarmWithVoice(message: String, sender: String) {
        // Create high-priority alarm notification
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("അപ്പച്ചാ! Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setFullScreenIntent(pendingIntent, true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        
        // Speak with AI voice - "Appacha, [message]"
        val voiceMessage = "അപ്പച്ചാ, $message"
        tts?.speak(voiceMessage, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    fun cleanup() {
        tts?.shutdown()
    }
}

/**
 * Reminder data class
 */
data class Reminder(
    val id: Int,
    val titleMl: String,
    val titleEn: String,
    val messageMl: String,
    val messageEn: String,
    val time: Long,
    val repeatType: String = "once", // once, daily, weekly
    val isActive: Boolean = true,
    val fromCaregiver: String = ""
)
