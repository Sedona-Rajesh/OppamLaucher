package com.oppam.oppamlauncher.location

import android.content.Context
import android.telephony.SmsManager
import com.oppam.oppamlauncher.auth.UserPreferences
import com.oppam.oppamlauncher.util.OppamLogger

class SMSLocationTransmitter : LocationTransmitter {
    override fun sendLocationUpdate(
        context: Context,
        lat: Double,
        lng: Double,
        accuracy: Float,
        timestamp: Long
    ) {
        val caregiverPhone = UserPreferences(context).getCaregiverPhone()
        if (caregiverPhone.isBlank()) {
            OppamLogger.w("Caregiver phone not set; skipping SMS location update")
            return
        }
        val msg = "OPPAM_LOC:${lat},${lng}|${accuracy}|${timestamp}"
        try {
            val smsManager: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(caregiverPhone, null, msg, null, null)
            OppamLogger.i("Sent location SMS to caregiver: $msg")
        } catch (e: Exception) {
            OppamLogger.e("Failed to send location SMS", e)
        }
    }
}
