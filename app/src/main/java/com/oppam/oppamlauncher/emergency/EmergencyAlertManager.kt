package com.oppam.oppamlauncher.emergency

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.telephony.SmsManager

/**
 * EMERGENCY ALERT DELIVERY SYSTEM
 * 
 * Handles sending alerts to caregivers when distress is detected.
 * Uses SMS as the primary notification method for reliability.
 * 
 * PRIVACY: Only sends minimal alert information, no call content.
 */
class EmergencyAlertManager(private val context: Context) {
    
    private val prefs = EmergencyCallPreferences(context)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    
    /**
     * Send an emergency alert to all registered caregivers.
     * 
     * @param alertType Type of distress detected
     * @param severity Severity level (LOW, MEDIUM, HIGH)
     * @param additionalInfo Optional additional context
     */
    fun sendEmergencyAlert(
        alertType: AlertType,
        severity: DistressSeverity,
        additionalInfo: String = ""
    ) {
        // Log alert (no sensitive data)
        logAlert(alertType, severity)
        
        // Vibrate device to alert user
        vibrateDevice(severity)
        
        // SMS disabled (alarm-only mode). Keep vibration/logging only.
    }
    
    /**
     * Send manual panic alert (user pressed SOS button).
     */
    fun sendManualPanicAlert() {
        sendEmergencyAlert(
            alertType = AlertType.MANUAL_PANIC,
            severity = DistressSeverity.HIGH,
            additionalInfo = "Manual SOS triggered during call"
        )
    }
    
    private fun sendSmsAlerts(
        alertType: AlertType,
        severity: DistressSeverity,
        additionalInfo: String
    ) {
        // Get registered family members from preferences
        // (In production, this would come from the family list in MainActivity)
        val emergencyContacts = getEmergencyContacts()
        
        if (emergencyContacts.isEmpty()) {
            android.util.Log.w("EmergencyAlert", "No emergency contacts registered")
            return
        }
        
        val message = buildAlertMessage(alertType, severity, additionalInfo)
        
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            emergencyContacts.forEach { phoneNumber ->
                try {
                    smsManager.sendTextMessage(
                        phoneNumber,
                        null,
                        message,
                        null,
                        null
                    )
                    android.util.Log.i("EmergencyAlert", "Alert sent to $phoneNumber")
                } catch (e: Exception) {
                    android.util.Log.e("EmergencyAlert", "Failed to send SMS to $phoneNumber", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EmergencyAlert", "SMS sending failed", e)
        }
    }
    
    private fun buildAlertMessage(
        alertType: AlertType,
        severity: DistressSeverity,
        additionalInfo: String
    ): String {
        val timestamp = java.text.SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            java.util.Locale.getDefault()
        ).format(java.util.Date())
        
        val severityEmoji = when (severity) {
            DistressSeverity.LOW -> "⚠️"
            DistressSeverity.MEDIUM -> "🚨"
            DistressSeverity.HIGH -> "🆘"
        }
        
        val alertDescription = when (alertType) {
            AlertType.KEYWORD_DETECTED -> "Emergency keyword detected in call"
            AlertType.LONG_SILENCE -> "Unusual silence during call"
            AlertType.VOICE_ANOMALY -> "Voice distress detected"
            AlertType.MANUAL_PANIC -> "MANUAL SOS - User pressed panic button"
            AlertType.SUDDEN_TERMINATION -> "Call ended abruptly after distress"
        }
        
        return """
            $severityEmoji OPPAM EMERGENCY ALERT
            
            $alertDescription
            Time: $timestamp
            Severity: $severity
            
            ${if (additionalInfo.isNotEmpty()) "Info: $additionalInfo\n" else ""}
            Please check on your family member immediately.
        """.trimIndent()
    }
    
    private fun vibrateDevice(severity: DistressSeverity) {
        if (vibrator == null || !vibrator.hasVibrator()) return
        
        val pattern = when (severity) {
            DistressSeverity.LOW -> longArrayOf(0, 200, 100, 200)
            DistressSeverity.MEDIUM -> longArrayOf(0, 300, 100, 300, 100, 300)
            DistressSeverity.HIGH -> longArrayOf(0, 500, 200, 500, 200, 500, 200, 500)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, -1)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
    
    private fun logAlert(alertType: AlertType, severity: DistressSeverity) {
        // Store only metadata, no sensitive information
        val timestamp = System.currentTimeMillis()
        
        // In production, store in local database for caregiver review
        android.util.Log.i(
            "EmergencyAlert",
            "Alert logged: type=$alertType, severity=$severity, time=$timestamp"
        )
        
        // Could be saved to SharedPreferences or Room database:
        // prefs.addAlertLog(AlertLog(alertType, severity, timestamp))
    }
    
    /**
     * Get emergency contact phone numbers.
     * In production, this should retrieve the family member list
     * from the main app's data store.
     */
    private fun getEmergencyContacts(): List<String> {
        // This is a placeholder. In production:
        // 1. Access the family member list from MainActivity's state
        // 2. Or store emergency contacts separately in preferences
        // 3. Or query from a shared database
        
        val contactsJson = prefs.getEmergencyContacts()
        if (contactsJson.isEmpty()) {
            return emptyList()
        }
        
        // Parse JSON or comma-separated list
        return contactsJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

/**
 * Types of emergency alerts.
 */
enum class AlertType {
    KEYWORD_DETECTED,       // "സഹായം", "HELP", etc. detected
    LONG_SILENCE,           // Unusual long silence during call
    VOICE_ANOMALY,          // Voice pitch/energy anomaly
    MANUAL_PANIC,           // User pressed SOS button
    SUDDEN_TERMINATION      // Call ended right after distress signal
}

/**
 * Severity levels for emergency alerts.
 */
enum class DistressSeverity {
    LOW,      // Minor anomaly, may be false positive
    MEDIUM,   // Moderate concern, requires attention
    HIGH      // Critical emergency, immediate action needed
}

/**
 * Data class for alert history (optional logging).
 */
data class AlertLog(
    val alertType: AlertType,
    val severity: DistressSeverity,
    val timestamp: Long,
    val wasManual: Boolean = false
)
