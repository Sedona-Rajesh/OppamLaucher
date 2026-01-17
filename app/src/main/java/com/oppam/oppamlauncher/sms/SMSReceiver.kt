package com.oppam.oppamlauncher.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.oppam.oppamlauncher.alarm.AlarmScheduler
import com.oppam.oppamlauncher.alarm.AlarmStorage
import com.oppam.oppamlauncher.auth.UserPreferences
import com.oppam.oppamlauncher.status.StatusTracker

/**
 * SMS Receiver for caregiver reminders
 * Intercepts SMS messages and triggers alarms with AI voice
 */
class SMSReceiver : BroadcastReceiver() {
    
    private val TAG = "SMSReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val userPrefs = UserPreferences(context)
            val alarmStorage = AlarmStorage(context)
            val statusTracker = StatusTracker(context)
            
            Log.d(TAG, "SMS received. Processing...")

            var isOppamMessage = false

            // Combine multipart SMS segments into a single body to avoid parsing issues
            val combinedBody = try {
                messages?.joinToString(separator = "") { it.displayMessageBody } ?: ""
            } catch (e: Exception) {
                Log.e(TAG, "Failed to combine SMS parts", e)
                ""
            }

            // Process the combined message once (prevents duplicate handling for multipart messages)
            try {
                val message = combinedBody
                Log.d(TAG, "Combined SMS body: '$message'")

                when {
                        message.startsWith("OPPAM:") -> {
                            isOppamMessage = true
                            Log.d(TAG, "Processing instant reminder - ABORTING SMS BROADCAST")
                            // Instant reminder
                            val reminderMessage = message.substringAfter("OPPAM:")
                            val reminderSystem = com.oppam.oppamlauncher.reminders.ReminderSystem(context)
                            reminderSystem.showAlarmWithVoice(reminderMessage, "Caregiver")
                            statusTracker.updateElderOnline()
                        }
                        message.startsWith("OPPAM_ALARM:") -> {
                            isOppamMessage = true
                            Log.d(TAG, "Processing scheduled alarm - ABORTING SMS BROADCAST")
                            // Scheduled alarm
                            val parts = message.substringAfter("OPPAM_ALARM:").split("|")
                            if (parts.size == 3) {
                                val alarmId = parts[0].toInt()
                                val timeInMillis = parts[1].toLong()
                                val alarmMessage = parts[2]
                                
                                Log.d(TAG, "Alarm parsed: ID=$alarmId, Time=$timeInMillis, Msg='$alarmMessage'")
                                
                                val alarm = AlarmStorage.ScheduledAlarm(alarmId, alarmMessage, timeInMillis)
                                alarmStorage.saveAlarm(alarm)
                                Log.d(TAG, "Alarm saved to storage.")
                                
                                AlarmScheduler.scheduleAlarm(context, alarmId, alarmMessage, timeInMillis)
                                Log.d(TAG, "Alarm scheduled with AlarmManager.")
                                
                                // ACK disabled to reduce SMS load per updated requirements.
                                
                                // Show immediate notification that alarm was scheduled
                                android.widget.Toast.makeText(
                                    context,
                                    "അലാറം ഷെഡ്യൂൾ ചെയ്തു: $alarmMessage",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Log.e(TAG, "Invalid OPPAM_ALARM format. Parts count: ${parts.size}")
                            }
                            statusTracker.updateElderOnline()
                        }
                        // Status SMS disabled: no online/offline pings via SMS.
                        message.startsWith("OPPAM_LOC:") -> {
                            isOppamMessage = true
                            Log.d(TAG, "Processing location update - ABORTING SMS BROADCAST")
                            try {
                                val payload = message.substringAfter("OPPAM_LOC:")
                                val parts = payload.split("|")
                                if (parts.size >= 3) {
                                    val latLng = parts[0].split(",")
                                    val lat = latLng[0].toDouble()
                                    val lng = latLng[1].toDouble()
                                    val acc = parts[1].toFloat()
                                    val ts = parts[2].toLong()
                                    val locStatus = com.oppam.oppamlauncher.status.LocationStatus(context)
                                    locStatus.update(lat, lng, acc, ts)
                                    Log.d(TAG, "Updated caregiver location cache: $lat,$lng ±$acc at $ts")
                                } else {
                                    Log.e(TAG, "Invalid OPPAM_LOC format")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to parse OPPAM_LOC", e)
                            }
                        }
                        else -> {
                            Log.d(TAG, "SMS is not for Oppam. Allowing normal SMS flow.")
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            }
            
            // CRITICAL: Abort broadcast to prevent OPPAM messages from appearing in SMS inbox
            if (isOppamMessage) {
                Log.d(TAG, "ABORTING BROADCAST - OPPAM message will NOT appear in SMS app")
                abortBroadcast()
            }
        }
    }
}
