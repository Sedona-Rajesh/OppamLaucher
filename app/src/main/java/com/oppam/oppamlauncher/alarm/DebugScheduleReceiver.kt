package com.oppam.oppamlauncher.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Debug receiver to schedule a test alarm from ADB.
 * Usage:
 * adb shell am broadcast -a com.oppam.oppamlauncher.SCHEDULE_TEST_ALARM --es message "Morning tablet" --ei delaySeconds 60 --ei id 999
 */
class DebugScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.oppam.oppamlauncher.SCHEDULE_TEST_ALARM") {
            val message = intent.getStringExtra("message") ?: "മരുന്ന് കഴിക്കണം"
            val delaySeconds = intent.getIntExtra("delaySeconds", 60)
            val alarmId = intent.getIntExtra("id", (System.currentTimeMillis() / 1000).toInt())
            val timeInMillis = System.currentTimeMillis() + delaySeconds * 1000L

            Log.d("DebugScheduleReceiver", "Scheduling test alarm id=$alarmId in ${delaySeconds}s: $message")

            val storage = AlarmStorage(context)
            storage.saveAlarm(
                AlarmStorage.ScheduledAlarm(
                    id = alarmId,
                    message = message,
                    timeInMillis = timeInMillis
                )
            )
            AlarmScheduler.scheduleAlarm(context, alarmId, message, timeInMillis)
        }
    }
}
