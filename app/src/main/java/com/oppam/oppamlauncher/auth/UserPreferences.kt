package com.oppam.oppamlauncher.auth

import android.content.Context
import android.content.SharedPreferences

/**
 * User Authentication & Profile Management
 */
class UserPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "user_prefs",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_TYPE = "user_type"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_LINKED_ELDER_PHONE = "linked_elder_phone"
        private const val KEY_LINKED_ELDER_NAME = "linked_elder_name"
        private const val KEY_CAREGIVER_PHONE = "caregiver_phone"
        private const val KEY_CAREGIVER_NAME = "caregiver_name"
    }
    
    // Login state
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    
    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()
    }
    
    // User type: "ELDER" or "CAREGIVER"
    fun getUserType(): String = prefs.getString(KEY_USER_TYPE, "") ?: ""
    
    fun setUserType(type: String) {
        prefs.edit().putString(KEY_USER_TYPE, type).apply()
    }
    
    // User profile
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    
    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }
    
    fun getUserPhone(): String = prefs.getString(KEY_USER_PHONE, "") ?: ""
    
    fun setUserPhone(phone: String) {
        prefs.edit().putString(KEY_USER_PHONE, phone).apply()
    }
    
    // For CAREGIVER: Link to elder
    fun getLinkedElderPhone(): String = prefs.getString(KEY_LINKED_ELDER_PHONE, "") ?: ""
    
    fun setLinkedElderPhone(phone: String) {
        prefs.edit().putString(KEY_LINKED_ELDER_PHONE, phone).apply()
    }
    
    fun getLinkedElderName(): String = prefs.getString(KEY_LINKED_ELDER_NAME, "") ?: ""
    
    fun setLinkedElderName(name: String) {
        prefs.edit().putString(KEY_LINKED_ELDER_NAME, name).apply()
    }
    
    // For ELDER: Caregiver contact
    fun getCaregiverPhone(): String = prefs.getString(KEY_CAREGIVER_PHONE, "") ?: ""
    
    fun setCaregiverPhone(phone: String) {
        prefs.edit().putString(KEY_CAREGIVER_PHONE, phone).apply()
    }
    
    fun getCaregiverName(): String = prefs.getString(KEY_CAREGIVER_NAME, "") ?: ""
    
    fun setCaregiverName(name: String) {
        prefs.edit().putString(KEY_CAREGIVER_NAME, name).apply()
    }
    
    // Complete registration
    fun registerUser(
        name: String,
        phone: String,
        userType: String,
        caregiverPhone: String = "",
        caregiverName: String = ""
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_TYPE, userType)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_PHONE, phone)
            
            if (userType == "ELDER" && caregiverPhone.isNotEmpty()) {
                putString(KEY_CAREGIVER_PHONE, caregiverPhone)
                putString(KEY_CAREGIVER_NAME, caregiverName)
            }
            
            apply()
        }
    }
    
    // Logout
    fun logout() {
        prefs.edit().clear().apply()
    }
    
    // Clear all data (for logout)
    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}

data class User(
    val name: String,
    val phone: String,
    val type: String, // "ELDER" or "CAREGIVER"
    val caregiverPhone: String = "",
    val caregiverName: String = ""
)
