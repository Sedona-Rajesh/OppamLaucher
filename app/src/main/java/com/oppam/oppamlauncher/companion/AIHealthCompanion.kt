package com.oppam.oppamlauncher.companion

import android.content.Context
import android.speech.tts.TextToSpeech
import com.oppam.oppamlauncher.auth.UserPreferences
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * AI Health Companion - Regular check-ins with elderly users
 * Makes them feel cared for and not alone
 */
class AIHealthCompanion(private val context: Context) {
    
    private val prefs = CompanionPreferences(context)
    private val userPrefs = UserPreferences(context)
    private var tts: TextToSpeech? = null
    private var checkInJob: Job? = null
    
    companion object {
        // Check-in intervals (in hours)
        const val MORNING_CHECKIN = 8 // 8 AM
        const val AFTERNOON_CHECKIN = 14 // 2 PM
        const val EVENING_CHECKIN = 20 // 8 PM
    }
    
    init {
        initializeTTS()
    }
    
    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale("ml", "IN") // Malayalam
                if (tts?.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                    tts?.language = locale
                }
            }
        }
    }
    
    /**
     * Start the AI companion service
     */
    fun startCompanion() {
        checkInJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                checkIfTimeForCheckIn()
                delay(60_000) // Check every minute
            }
        }
    }
    
    /**
     * Stop the AI companion
     */
    fun stopCompanion() {
        checkInJob?.cancel()
    }
    
    /**
     * Check if it's time for a health check-in
     */
    private fun checkIfTimeForCheckIn() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val lastCheckIn = prefs.getLastCheckInTime()
        
        // Check if we haven't done a check-in today at this time
        val shouldCheckIn = when (currentHour) {
            MORNING_CHECKIN -> !prefs.hasCheckedInToday("morning")
            AFTERNOON_CHECKIN -> !prefs.hasCheckedInToday("afternoon")
            EVENING_CHECKIN -> !prefs.hasCheckedInToday("evening")
            else -> false
        }
        
        if (shouldCheckIn) {
            performCheckIn(getCheckInType(currentHour))
        }
    }
    
    private fun getCheckInType(hour: Int): String {
        return when (hour) {
            MORNING_CHECKIN -> "morning"
            AFTERNOON_CHECKIN -> "afternoon"
            EVENING_CHECKIN -> "evening"
            else -> "general"
        }
    }
    
    /**
     * Perform a health check-in
     */
    fun performCheckIn(type: String) {
        val userName = userPrefs.getUserName()
        val greeting = getGreeting(type, userName)
        val question = getHealthQuestion(type)
        
        val message = "$greeting $question"
        
        // Speak the message
        speak(message)
        
        // Mark check-in as done
        prefs.markCheckInDone(type)
        
        // Log the check-in
        prefs.logCheckIn(type, message)
    }
    
    /**
     * Generate greeting based on time of day
     */
    private fun getGreeting(type: String, name: String): String {
        val nameGreeting = if (name.isNotBlank()) "$name, " else "അപ്പച്ചാ, "
        
        return when (type) {
            "morning" -> "${nameGreeting}സുപ്രഭാതം!"
            "afternoon" -> "${nameGreeting}നല്ല ഉച്ചയ്ക്ക് ആശംസകൾ!"
            "evening" -> "${nameGreeting}സന്ധ്യാ ആശംസകൾ!"
            else -> nameGreeting
        }
    }
    
    /**
     * Generate health question based on context
     */
    private fun getHealthQuestion(type: String): String {
        val questions = when (type) {
            "morning" -> listOf(
                "ഇന്ന് എങ്ങനെയുണ്ട്?",
                "രാത്രി നല്ല ഉറക്കം ഉണ്ടായോ?",
                "പ്രഭാതഭക്ഷണം കഴിച്ചോ?",
                "ശരീരം സുഖമാണോ?"
            )
            "afternoon" -> listOf(
                "ഉച്ചഭക്ഷണം കഴിച്ചോ?",
                "മരുന്ന് കഴിച്ചോ?",
                "എന്തെങ്കിലും വേദനയുണ്ടോ?",
                "വെള്ളം കുടിച്ചോ?"
            )
            "evening" -> listOf(
                "ഇന്ന് എങ്ങനെ പോയി?",
                "സന്ധ്യയ്ക്ക് എന്തെങ്കിലും വേണോ?",
                "രാത്രി മരുന്ന് കഴിച്ചോ?",
                "എന്തെങ്കിലും പ്രശ്നമുണ്ടോ?"
            )
            else -> listOf("സുഖമാണോ?")
        }
        
        return questions.random()
    }
    
    /**
     * Speak a message using TTS
     */
    fun speak(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_ADD, null, null)
    }
    
    /**
     * Log health data
     */
    fun logHealthData(type: String, value: String) {
        prefs.logHealthData(type, value)
        
        // Provide encouraging response
        val response = when (type) {
            "medicine_taken" -> "നല്ലത്! മരുന്ന് കഴിച്ചല്ലോ."
            "meal_eaten" -> "വളരെ നല്ലത്!"
            "feeling_good" -> "സന്തോഷം!"
            "pain" -> "എന്തോ വേദനയുണ്ടോ? കുടുംബാംഗത്തെ വിളിക്കണോ?"
            else -> "നന്നായിരിക്കുന്നു."
        }
        
        speak(response)
    }
    
    fun cleanup() {
        tts?.shutdown()
        stopCompanion()
    }
}
