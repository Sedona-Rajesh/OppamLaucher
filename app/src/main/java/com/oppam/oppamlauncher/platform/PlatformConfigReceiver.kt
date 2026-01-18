package com.oppam.oppamlauncher.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.oppam.oppamlauncher.auth.UserPreferences

/**
 * Allows setting the caregiver platform webhook URL via adb broadcast.
 * Usage:
 * adb shell am broadcast -a com.oppam.oppamlauncher.SET_PLATFORM_URL --es url "https://example.com/webhook"
 */
class PlatformConfigReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.oppam.oppamlauncher.SET_PLATFORM_URL") {
            val url = intent.getStringExtra("url") ?: ""
            val prefs = UserPreferences(context)
            if (url.isNotBlank()) {
                prefs.setCaregiverPlatformUrl(url)
                Log.d("PlatformConfigReceiver", "Caregiver platform URL set to: $url")
                Toast.makeText(context, "Platform URL set", Toast.LENGTH_SHORT).show()
            } else {
                Log.w("PlatformConfigReceiver", "Empty platform URL provided")
                Toast.makeText(context, "Empty platform URL", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
