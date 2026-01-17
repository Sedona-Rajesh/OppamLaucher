package com.oppam.oppamlauncher.emergency

import android.content.Context

/**
 * DISTRESS ANALYZER - REAL-TIME CALL ANALYSIS
 * 
 * Analyzes call audio in real-time to detect signs of distress.
 * 
 * CRITICAL PRIVACY RULES:
 * - NO audio recording or storage
 * - Analysis is ephemeral (in-memory only)
 * - Only outputs boolean flags or severity levels
 * - Raw audio data is immediately discarded after analysis
 * 
 * DETECTION METHODS:
 * 1. Keyword Detection: "സഹായം", "HELP", "EMERGENCY", "വേദന", "PAIN"
 * 2. Silence Detection: Abnormally long pauses
 * 3. Voice Anomaly: Sudden pitch or energy changes (future enhancement)
 */
class DistressAnalyzer(private val context: Context) {
    
    // Malayalam emergency keywords
    private val malayalamKeywords = listOf(
        "സഹായം",      // Help
        "വേദന",       // Pain
        "അപകടം",      // Danger
        "ആവശ്യം",     // Need/urgent
        "ബുദ്ധിമുട്ട്"  // Difficulty
    )
    
    // English emergency keywords
    private val englishKeywords = listOf(
        "help",
        "emergency",
        "pain",
        "hurt",
        "danger",
        "sick",
        "911"
    )
    
    // Silence threshold (in seconds)
    private val LONG_SILENCE_THRESHOLD = 15
    
    // Last audio activity timestamp
    private var lastAudioActivity: Long = System.currentTimeMillis()
    private var silenceStartTime: Long? = null
    
    /**
     * Analyze text from speech recognition for distress keywords.
     * 
     * NOTE: In production, this would be called with real-time speech-to-text output.
     * The actual audio is NOT stored, only the transcription for keyword matching.
     * 
     * @param text Transcribed text from speech recognition
     * @return DistressResult indicating if distress was detected
     */
    fun analyzeKeywords(text: String): DistressResult {
        val lowerText = text.lowercase()
        
        // Check Malayalam keywords
        val malayalamMatch = malayalamKeywords.any { keyword ->
            lowerText.contains(keyword)
        }
        
        // Check English keywords
        val englishMatch = englishKeywords.any { keyword ->
            lowerText.contains(keyword)
        }
        
        return when {
            malayalamMatch || englishMatch -> {
                DistressResult(
                    isDistress = true,
                    alertType = AlertType.KEYWORD_DETECTED,
                    severity = DistressSeverity.HIGH,
                    details = "Emergency keyword detected in conversation"
                )
            }
            else -> {
                DistressResult(
                    isDistress = false,
                    alertType = null,
                    severity = DistressSeverity.LOW,
                    details = "No distress keywords detected"
                )
            }
        }
    }
    
    /**
     * Track silence duration during the call.
     * 
     * @param isAudioActive Whether audio/speech is currently active
     * @return DistressResult if abnormal silence is detected
     */
    fun analyzeSilence(isAudioActive: Boolean): DistressResult {
        val currentTime = System.currentTimeMillis()
        
        if (isAudioActive) {
            // Reset silence tracking
            lastAudioActivity = currentTime
            silenceStartTime = null
            
            return DistressResult(
                isDistress = false,
                alertType = null,
                severity = DistressSeverity.LOW,
                details = "Audio active"
            )
        } else {
            // Track silence duration
            if (silenceStartTime == null) {
                silenceStartTime = currentTime
            }
            
            val silenceDuration = (currentTime - (silenceStartTime ?: currentTime)) / 1000
            
            return if (silenceDuration > LONG_SILENCE_THRESHOLD) {
                DistressResult(
                    isDistress = true,
                    alertType = AlertType.LONG_SILENCE,
                    severity = DistressSeverity.MEDIUM,
                    details = "Unusual silence detected: ${silenceDuration}s"
                )
            } else {
                DistressResult(
                    isDistress = false,
                    alertType = null,
                    severity = DistressSeverity.LOW,
                    details = "Normal silence: ${silenceDuration}s"
                )
            }
        }
    }
    
    /**
     * Analyze voice audio characteristics (pitch, energy, etc.)
     * 
     * NOTE: This is a placeholder for future ML-based analysis.
     * In production, this would use audio processing libraries to detect
     * voice anomalies WITHOUT storing the audio.
     * 
     * @param audioBuffer Raw audio samples (NOT stored)
     * @return DistressResult based on voice analysis
     */
    fun analyzeVoiceCharacteristics(audioBuffer: ShortArray): DistressResult {
        // PLACEHOLDER: Future implementation would use audio signal processing
        // to detect pitch anomalies, sudden volume changes, trembling voice, etc.
        
        // Calculate basic energy level (volume)
        val energy = calculateAudioEnergy(audioBuffer)
        
        // In a real implementation:
        // - Extract pitch using FFT or autocorrelation
        // - Compare with baseline voice characteristics
        // - Detect sudden changes or trembling
        // - Use ML model for distress classification
        
        // For now, return non-distress
        return DistressResult(
            isDistress = false,
            alertType = null,
            severity = DistressSeverity.LOW,
            details = "Voice analysis: normal (energy=$energy)"
        )
    }
    
    /**
     * Calculate audio energy level (simple RMS).
     * This is ephemeral - audio data is not stored.
     */
    private fun calculateAudioEnergy(audioBuffer: ShortArray): Double {
        var sum = 0.0
        for (sample in audioBuffer) {
            sum += sample * sample
        }
        return kotlin.math.sqrt(sum / audioBuffer.size)
    }
    
    /**
     * Reset analyzer state (called when call ends).
     */
    fun reset() {
        lastAudioActivity = System.currentTimeMillis()
        silenceStartTime = null
    }
}

/**
 * Result of distress analysis.
 */
data class DistressResult(
    val isDistress: Boolean,
    val alertType: AlertType?,
    val severity: DistressSeverity,
    val details: String
)
