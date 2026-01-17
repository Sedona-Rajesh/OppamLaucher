package com.oppam.oppamlauncher.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * SMS Sender for caregivers to send reminders to elderly
 */
object SMSSender {
    
    /**
     * Send reminder SMS to elderly phone
     */
    fun sendReminder(context: Context, phoneNumber: String, message: String): Boolean {
        // Check SMS permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "SMS permission not granted. Please enable in Settings.", Toast.LENGTH_LONG).show()
            return false
        }
        
        return try {
            // Add OPPAM prefix so receiver knows it's a reminder
            val formattedMessage = "OPPAM:$message"
            
            // Use context-based SMS manager for Android 6+
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            smsManager.sendTextMessage(
                phoneNumber,
                null,
                formattedMessage,
                null,
                null
            )
            
            Toast.makeText(context, "Reminder sent to $phoneNumber!", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to send SMS: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }
    
    /**
     * Request status update from elderly phone
     */
    fun requestStatusUpdate(context: Context, phoneNumber: String) {
        // Check SMS permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "SMS permission not granted. Please enable in Settings.", Toast.LENGTH_LONG).show()
            return
        }
        
        try {
            // Use context-based SMS manager for Android 6+
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            smsManager.sendTextMessage(
                phoneNumber,
                null,
                "OPPAM_STATUS_REQUEST",
                null,
                null
            )
        } catch (e: Exception) {
            // Silent failure
        }
    }
    
    /**
     * Send status update to caregiver
     */
    fun sendStatusUpdate(context: Context, phoneNumber: String, message: String) {
        // Check SMS permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "SMS permission not granted. Please enable in Settings.", Toast.LENGTH_LONG).show()
            return
        }
        
        try {
            // Add OPPAM prefix so receiver knows it's a status update
            val formattedMessage = "OPPAM_STATUS_UPDATE:$message"
            
            // Use context-based SMS manager for Android 6+
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            smsManager.sendTextMessage(
                phoneNumber,
                null,
                formattedMessage,
                null,
                null
            )
        } catch (e: Exception) {
            // Silent failure
        }
    }
}
