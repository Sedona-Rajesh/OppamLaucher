package com.oppam.oppamlauncher.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 * Utility receiver to clear user data and relaunch login screen.
 * Trigger with: adb shell am broadcast -a com.oppam.oppamlauncher.FORCE_LOGOUT
 */
class ForceLogoutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            Log.d("ForceLogoutReceiver", "Received FORCE_LOGOUT broadcast; clearing user data")
            val userPrefs = UserPreferences(context)
            userPrefs.clearAllData()
            Toast.makeText(context, "Oppam: reset complete. Please login again.", Toast.LENGTH_LONG).show()

            // Launch MainActivity to show login UI
            val launch = Intent(context, com.oppam.oppamlauncher.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(launch)
        } catch (e: Exception) {
            Log.e("ForceLogoutReceiver", "Failed to clear data", e)
        }
    }
}
