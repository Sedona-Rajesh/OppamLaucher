package com.oppam.oppamlauncher.emergency

import android.content.Context
import android.content.SharedPreferences

/**
 * EMERGENCY CALL FEATURE PREFERENCES
 * 
 * Manages user consent and feature settings for the emergency call monitoring.
 * All settings are stored locally on the device.
 * 
 * PRIVACY: No sensitive data is stored, only boolean flags and emergency contact numbers.
 */
class EmergencyCallPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "emergency_call_prefs"
        private const val KEY_CONSENT_GIVEN = "consent_given"
        private const val KEY_FEATURE_ENABLED = "feature_enabled"
        private const val KEY_CONSENT_DECLINED = "consent_declined"
        private const val KEY_EMERGENCY_CONTACTS = "emergency_contacts"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }
    
    /**
     * Check if user has given consent to use the emergency call feature.
     */
    fun hasConsentGiven(): Boolean {
        return prefs.getBoolean(KEY_CONSENT_GIVEN, false)
    }
    
    /**
     * Record that user has given consent.
     */
    fun setConsentGiven(consent: Boolean) {
        prefs.edit().putBoolean(KEY_CONSENT_GIVEN, consent).apply()
    }
    
    /**
     * Check if the emergency call monitoring feature is currently enabled.
     */
    fun isFeatureEnabled(): Boolean {
        return prefs.getBoolean(KEY_FEATURE_ENABLED, false)
    }
    
    /**
     * Enable or disable the emergency call monitoring feature.
     */
    fun setFeatureEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FEATURE_ENABLED, enabled).apply()
    }
    
    /**
     * Check if user has explicitly declined consent.
     */
    fun hasDeclinedConsent(): Boolean {
        return prefs.getBoolean(KEY_CONSENT_DECLINED, false)
    }
    
    /**
     * Record that user has declined consent (don't ask again).
     */
    fun setDeclineConsent(declined: Boolean) {
        prefs.edit().putBoolean(KEY_CONSENT_DECLINED, declined).apply()
    }
    
    /**
     * Get the list of emergency contact phone numbers (comma-separated).
     */
    fun getEmergencyContacts(): String {
        return prefs.getString(KEY_EMERGENCY_CONTACTS, "") ?: ""
    }
    
    /**
     * Set the list of emergency contact phone numbers (comma-separated).
     */
    fun setEmergencyContacts(contacts: String) {
        prefs.edit().putString(KEY_EMERGENCY_CONTACTS, contacts).apply()
    }
    
    /**
     * Add a single emergency contact to the list.
     */
    fun addEmergencyContact(phoneNumber: String) {
        val current = getEmergencyContacts()
        val updated = if (current.isEmpty()) {
            phoneNumber
        } else {
            "$current,$phoneNumber"
        }
        setEmergencyContacts(updated)
    }
    
    /**
     * Set primary emergency contact (usually the caregiver).
     */
    fun setEmergencyContact(phoneNumber: String) {
        setEmergencyContacts(phoneNumber)
    }
    
    /**
     * Remove a specific emergency contact from the list.
     */
    fun removeEmergencyContact(phoneNumber: String) {
        val current = getEmergencyContacts()
        val contacts = current.split(",").toMutableList()
        contacts.remove(phoneNumber)
        setEmergencyContacts(contacts.joinToString(","))
    }
    
    /**
     * Get list of emergency contacts as a list.
     */
    fun getEmergencyContactsList(): List<String> {
        val contactsString = getEmergencyContacts()
        if (contactsString.isEmpty()) return emptyList()
        return contactsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    
    /**
     * Check if this is the first time the app is launched (for consent prompt).
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    /**
     * Mark that the app has been launched before.
     */
    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }
    
    /**
     * Reset all emergency call preferences (for testing or user request).
     */
    fun resetAllPreferences() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Check if the feature is fully configured and ready to use.
     */
    fun isFullyConfigured(): Boolean {
        return hasConsentGiven() && 
               isFeatureEnabled() && 
               getEmergencyContactsList().isNotEmpty()
    }
}
