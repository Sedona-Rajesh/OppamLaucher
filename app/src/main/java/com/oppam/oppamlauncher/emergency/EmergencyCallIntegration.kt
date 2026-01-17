package com.oppam.oppamlauncher.emergency

import android.content.Context
import androidx.compose.runtime.*
import com.oppam.oppamlauncher.emergency.ui.EmergencyCallConsentScreen
import com.oppam.oppamlauncher.emergency.ui.EmergencyCallSettingsScreen
import com.oppam.oppamlauncher.emergency.ui.ActiveMonitoringIndicator

/**
 * EMERGENCY CALL FEATURE INTEGRATION LAYER
 * 
 * This is the ONLY integration point between the emergency call monitoring
 * feature and the main app. It ensures complete isolation and zero impact
 * on existing functionality.
 * 
 * PRIVACY-FIRST DESIGN:
 * - No audio recording
 * - Real-time analysis only
 * - Explicit consent required
 * - Can be disabled at any time
 */
object EmergencyCallIntegration {
    
    private var monitorService: EmergencyCallMonitorService? = null
    
    /**
     * Initialize the emergency call monitoring service.
     * This is called ONLY if the user has given consent and enabled the feature.
     * 
     * @param context Application context
     * @return true if initialized successfully, false otherwise
     */
    fun initializeIfEnabled(context: Context): Boolean {
        val prefs = EmergencyCallPreferences(context)
        
        // Check if user has consented and feature is enabled
        if (!prefs.hasConsentGiven() || !prefs.isFeatureEnabled()) {
            return false
        }
        
        // Initialize service if not already running
        if (monitorService == null) {
            monitorService = EmergencyCallMonitorService(context)
        }
        
        return true
    }
    
    /**
     * Stop the emergency call monitoring service.
     * Called when feature is disabled or app is destroyed.
     */
    fun stopMonitoring() {
        monitorService?.stopMonitoring()
        monitorService = null
    }
    
    /**
     * Check if the feature is currently active.
     */
    fun isActive(context: Context): Boolean {
        val prefs = EmergencyCallPreferences(context)
        return prefs.isFeatureEnabled() && monitorService != null
    }
    
    /**
     * Check if user needs to see the consent screen.
     */
    fun needsConsent(context: Context): Boolean {
        val prefs = EmergencyCallPreferences(context)
        return !prefs.hasConsentGiven()
    }
}

/**
 * Composable function to handle emergency call feature UI flow.
 * This can be added to the main app without modifying existing screens.
 * 
 * Returns a state indicating which screen to show (if any):
 * - "CONSENT" - Show consent screen
 * - "ACTIVE" - Feature is active, show indicator
 * - "NONE" - Don't show anything
 */
@Composable
fun EmergencyCallFeatureHandler(
    context: Context,
    onNavigateToSettings: () -> Unit
): String {
    val prefs = remember { EmergencyCallPreferences(context) }
    var showConsent by remember { mutableStateOf(!prefs.hasConsentGiven()) }
    var featureEnabled by remember { mutableStateOf(prefs.isFeatureEnabled()) }
    
    return when {
        showConsent -> "CONSENT"
        featureEnabled -> "ACTIVE"
        else -> "NONE"
    }
}

/**
 * Optional: Add emergency call settings to caregiver dashboard.
 * This is a completely separate composable that can be added without
 * modifying existing UI.
 */
@Composable
fun EmergencyCallSettingsEntry(
    onClick: () -> Unit
) {
    // This can be added to the caregiver dashboard as an optional menu item
    // Implementation in EmergencyCallUI.kt
}
