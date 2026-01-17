package com.oppam.oppamlauncher.emergency

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * CALL STATE LISTENER - PHONE CALL EVENT MONITORING
 * 
 * Listens for phone call state changes (incoming, answered, ended)
 * WITHOUT interfering with the call itself.
 * 
 * PRIVACY: Only monitors call state (idle/ringing/off-hook),
 * does NOT access call audio unless emergency monitoring is active.
 */
class CallStateListener(
    private val context: Context,
    private val onCallStateChanged: (CallState) -> Unit
) {
    
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    
    // For Android 12+ (API 31+)
    @RequiresApi(Build.VERSION_CODES.S)
    private inner class ModernCallStateCallback : TelephonyCallback(), 
        TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            handleCallStateChange(state)
        }
    }
    
    // For Android 11 and below
    @Suppress("DEPRECATION")
    private val legacyPhoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            handleCallStateChange(state)
        }
    }
    
    private var modernCallback: ModernCallStateCallback? = null
    
    /**
     * Start listening for call state changes.
     */
    fun startListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            modernCallback = ModernCallStateCallback()
            telephonyManager.registerTelephonyCallback(
                context.mainExecutor,
                modernCallback!!
            )
        } else {
            // Android 11 and below
            @Suppress("DEPRECATION")
            telephonyManager.listen(
                legacyPhoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE
            )
        }
    }
    
    /**
     * Stop listening for call state changes.
     */
    fun stopListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modernCallback?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
            modernCallback = null
        } else {
            @Suppress("DEPRECATION")
            telephonyManager.listen(
                legacyPhoneStateListener,
                PhoneStateListener.LISTEN_NONE
            )
        }
    }
    
    /**
     * Handle call state changes and convert to our CallState enum.
     */
    private fun handleCallStateChange(state: Int) {
        val callState = when (state) {
            TelephonyManager.CALL_STATE_IDLE -> CallState.IDLE
            TelephonyManager.CALL_STATE_RINGING -> CallState.RINGING
            TelephonyManager.CALL_STATE_OFFHOOK -> CallState.ACTIVE
            else -> CallState.IDLE
        }
        
        onCallStateChanged(callState)
    }
}

/**
 * Simplified call states for emergency monitoring.
 */
enum class CallState {
    IDLE,       // No call activity
    RINGING,    // Incoming call (not answered yet)
    ACTIVE      // Call in progress (answered)
}
