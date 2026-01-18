package com.oppam.oppamlauncher.alarm

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.telephony.SmsManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oppam.oppamlauncher.auth.UserPreferences
import com.oppam.oppamlauncher.ui.theme.OppamLauncherTheme
import java.text.SimpleDateFormat
import java.util.*

class AlarmActivity : ComponentActivity() {
    
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val TAG = "AlarmActivity"
    private val handler = Handler(Looper.getMainLooper())
    private var noResponseTimeoutRunnable: Runnable? = null
    private var reRingRunnable: Runnable? = null
    private var alarmId: Int = -1
    private var message: String = ""
    private var scheduledTime: Long = 0
    private var actualTriggerTime: Long = 0
    private lateinit var showButtonsState: MutableState<Boolean>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        actualTriggerTime = System.currentTimeMillis()
        message = intent.getStringExtra("MESSAGE") ?: "അപ്പച്ചാ!"
        alarmId = intent.getIntExtra("ALARM_ID", -1)
        scheduledTime = intent.getLongExtra("SCHEDULED_TIME", 0)
        
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        Log.d(TAG, "========== ALARM TRIGGERED ==========")
        Log.d(TAG, "Alarm ID: $alarmId")
        Log.d(TAG, "Message: $message")
        Log.d(TAG, "Scheduled Time: ${sdf.format(Date(scheduledTime))}")
        Log.d(TAG, "Actual Trigger Time: ${sdf.format(Date(actualTriggerTime))}")
        Log.d(TAG, "Time Difference: ${actualTriggerTime - scheduledTime}ms")
        Log.d(TAG, "====================================")
        
        // Acquire wake lock to keep screen on
        acquireWakeLock()
        
        // Show over lockscreen and turn screen on
        setupWindowFlags()
        
        // Start alarm sound (3-5 seconds)
        startAlarmSound()
        
        // Start vibration
        startVibration()
        
        // Initialize TTS with callback sequence
        initializeTTSWithSequence()
        
        // Create UI state
        showButtonsState = mutableStateOf(false)
        
        setContent {
            OppamLauncherTheme {
                AlarmVerificationScreen(
                    message = message,
                    showButtons = showButtonsState.value,
                    onYes = { handleYesResponse() },
                    onNo = { handleNoButton() }
                )
            }
        }
        // Show verification buttons immediately on first ring
        showVerificationButtons()
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            "OppamLauncher:AlarmWakeLock"
        ).apply {
            acquire(5 * 60 * 1000L) // 5 minutes max
        }
        Log.d(TAG, "Wake lock acquired")
    }
    
    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        Log.d(TAG, "Window flags set for lockscreen override")
    }
    
    private fun startAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = false // Play once for 3-5 seconds
                prepare()
                start()
                
                setOnCompletionListener {
                    Log.d(TAG, "Alarm tone completed (3-5 seconds)")
                }
            }
            
            // Stop alarm sound after 5 seconds
            handler.postDelayed({
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                Log.d(TAG, "Alarm tone stopped after 5 seconds")
            }, 5000)
            
            Log.d(TAG, "Alarm sound started (will play for 5 seconds)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm sound", e)
        }
    }
    
    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        
        val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(pattern, 0) // Repeat
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
        Log.d(TAG, "Vibration started")
    }
    
    private fun initializeTTSWithSequence() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d(TAG, "TTS initialized successfully")
                val locale = Locale("ml", "IN")
                val result = tts?.setLanguage(locale)
                
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS Malayalam not supported - fallback to alarm tone only")
                } else {
                    Log.d(TAG, "TTS Malayalam language set successfully")
                    // After 5 seconds (when alarm tone stops), speak the reminder message
                    handler.postDelayed({
                        speakReminderMessage()
                    }, 5000)
                }
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
            }
        }
    }
    
    private fun speakReminderMessage() {
        val voiceMessage = "അപ്പച്ചാ! $message"
        Log.d(TAG, "Speaking reminder message: $voiceMessage")
        
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS started speaking reminder")
            }
            
            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS finished speaking reminder")
                // After reminder message, wait 10 seconds then ask verification question
                handler.postDelayed({
                    speakVerificationQuestion()
                }, 10000)
            }
            
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "TTS error while speaking reminder")
            }
        })
        
        val params = Bundle()
        tts?.speak(voiceMessage, TextToSpeech.QUEUE_FLUSH, params, "ReminderMessage")
    }
    
    private fun speakVerificationQuestion() {
        val verificationMessage = "അപ്പച്ചാ, മരുന്ന് കഴിച്ചോ?" // "Did you take the medicine?"
        Log.d(TAG, "Speaking verification question: $verificationMessage")
        
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS started speaking verification question")
            }
            
            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS finished speaking verification question")
            }
            
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "TTS error while speaking verification")
            }
        })
        
        val params = Bundle()
        tts?.speak(verificationMessage, TextToSpeech.QUEUE_FLUSH, params, "VerificationQuestion")
    }
    
    private fun showVerificationButtons() {
        runOnUiThread {
            showButtonsState.value = true
            Log.d(TAG, "Verification buttons now visible; starting 2-stage timeouts")
        }

        // Cancel any previous timeouts
        noResponseTimeoutRunnable?.let { handler.removeCallbacks(it) }
        reRingRunnable?.let { handler.removeCallbacks(it) }

        // After 10 seconds without response, re-ring the alarm
        reRingRunnable = Runnable {
            Log.w(TAG, "No response after 10s - re-ringing alarm")
            reRingAlarm()
        }
        handler.postDelayed(reRingRunnable!!, 10_000)

        // After 20 seconds without response (10s + 10s), send SOS
        noResponseTimeoutRunnable = Runnable {
            Log.e(TAG, "No response after re-ring - triggering SOS")
            handleNoResponse()
        }
        handler.postDelayed(noResponseTimeoutRunnable!!, 20_000)
    }

    private fun reRingAlarm() {
        // Play the alarm tone again and vibrate to attract attention
        startAlarmSound()
        startVibration()
    }
    
    // Legacy timeout removed; timeouts are now scheduled when buttons appear
    
    private fun handleYesResponse() {
        Log.d(TAG, "========== YES BUTTON PRESSED ==========")
        Log.d(TAG, "Elder confirmed task completion")
        
        // Cancel no-response timeout
        noResponseTimeoutRunnable?.let { handler.removeCallbacks(it) }
        reRingRunnable?.let { handler.removeCallbacks(it) }
        
        // Mark alarm as confirmed
        if (alarmId != -1) {
            val alarmStorage = AlarmStorage(this)
            alarmStorage.updateAlarmStatus(alarmId, "confirmed")
        }
        
        // Send confirmation SMS to caregiver
        sendConfirmationSMS()
        
        // Stop alarm and finish
        stopAlarm()
        finish()
    }
    
    private fun handleNoButton() {
        Log.d(TAG, "========== NO BUTTON PRESSED ==========")
        Log.d(TAG, "Elder indicated task not completed")
        
        // Cancel no-response timeout
        noResponseTimeoutRunnable?.let { handler.removeCallbacks(it) }
        reRingRunnable?.let { handler.removeCallbacks(it) }
        
        // Mark alarm as not completed
        if (alarmId != -1) {
            val alarmStorage = AlarmStorage(this)
            alarmStorage.updateAlarmStatus(alarmId, "not_completed")
        }

        // Send notification SMS to caregiver
        sendNonCompletionSMS()

        // Reschedule next reminder according to interval and handle escalation
        rescheduleOrEscalate()

        // Stop current alarm UI
        stopAlarm()
        finish()
    }
    
    private fun handleNoResponse() {
        Log.e(TAG, "========== NO RESPONSE - SOS TRIGGERED ==========")
        Log.e(TAG, "Elder did not respond within 30 seconds - sending SOS")
        
        // Mark alarm as no response
        if (alarmId != -1) {
            val alarmStorage = AlarmStorage(this)
            alarmStorage.updateAlarmStatus(alarmId, "no_response")
        }
        
        // Reschedule next reminder according to interval and handle escalation
        rescheduleOrEscalate()

        // Stop alarm and finish
        stopAlarm()
        finish()
    }
    
    private fun sendConfirmationSMS() {
        try {
            val userPrefs = UserPreferences(this)
            val caregiverPhone = userPrefs.getCaregiverPhone()
            val elderName = userPrefs.getUserName()
            
            if (caregiverPhone.isEmpty()) {
                Log.e(TAG, "Cannot send confirmation SMS - caregiver phone not set")
                return
            }
            
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val timeStr = sdf.format(Date(actualTriggerTime))
            
            val smsMessage = "✅ $elderName confirmed: $message (at $timeStr)"
            
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            smsManager.sendTextMessage(caregiverPhone, null, smsMessage, null, null)
            Log.d(TAG, "Confirmation SMS sent to caregiver: $caregiverPhone")
            Log.d(TAG, "Message: $smsMessage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send confirmation SMS", e)
        }
    }
    
    private fun sendNonCompletionSMS() {
        try {
            val userPrefs = UserPreferences(this)
            val caregiverPhone = userPrefs.getCaregiverPhone()
            val elderName = userPrefs.getUserName()
            
            if (caregiverPhone.isEmpty()) {
                Log.e(TAG, "Cannot send non-completion SMS - caregiver phone not set")
                return
            }
            
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val timeStr = sdf.format(Date(actualTriggerTime))
            
            val smsMessage = "⚠️ $elderName indicated NOT completed: $message (at $timeStr)"
            
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            smsManager.sendTextMessage(caregiverPhone, null, smsMessage, null, null)
            Log.d(TAG, "Non-completion SMS sent to caregiver: $caregiverPhone")
            Log.d(TAG, "Message: $smsMessage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send non-completion SMS", e)
        }
    }
    
    private fun sendSOSMessages() {
        try {
            val userPrefs = UserPreferences(this)
            val caregiverPhone = userPrefs.getCaregiverPhone()
            val elderName = userPrefs.getUserName()
            
            if (caregiverPhone.isEmpty()) {
                Log.e(TAG, "Cannot send SOS - caregiver phone not set")
                return
            }
            
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val timeStr = sdf.format(Date(actualTriggerTime))
            
            val sosMessage = "🆘 SOS: No response from $elderName for scheduled reminder: $message (at $timeStr). Please check on them immediately!"
            
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            // Send to primary caregiver
            smsManager.sendTextMessage(caregiverPhone, null, sosMessage, null, null)
            Log.d(TAG, "SOS SMS sent to primary caregiver: $caregiverPhone")
            Log.d(TAG, "SOS Message: $sosMessage")
            
            // TODO: Send to all family contacts when family contact feature is implemented
            // For now, only sending to primary caregiver
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SOS messages", e)
        }
    }

    private fun rescheduleOrEscalate() {
        if (alarmId == -1) return

        try {
            val storage = AlarmStorage(this)
            val existing = storage.getAlarm(alarmId)
            // Snooze/reschedule should be exactly 5 minutes regardless of original interval
            val snoozeSeconds = 300
            val maxMisses = existing?.maxMisses ?: 3

            val newMisses = storage.incrementMissedCount(alarmId)
            Log.d(TAG, "Missed count for $alarmId updated to $newMisses (max $maxMisses)")

            if (newMisses >= maxMisses) {
                Log.w(TAG, "Missed threshold reached; sending escalation SMS")
                sendEscalationSMS(newMisses, maxMisses)
            }

            val nextTime = System.currentTimeMillis() + snoozeSeconds * 1000L
            val updated = AlarmStorage.ScheduledAlarm(
                id = alarmId,
                message = message,
                timeInMillis = nextTime,
                intervalSeconds = existing?.intervalSeconds ?: snoozeSeconds,
                maxMisses = maxMisses,
                missedCount = newMisses,
                status = "scheduled"
            )
            storage.saveAlarm(updated)
            AlarmScheduler.scheduleAlarm(this, alarmId, message, nextTime)
            Log.d(TAG, "Rescheduled alarm $alarmId for ${Date(nextTime)} with fixed snooze 300s (5 min)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule or escalate", e)
        }
    }

    private fun sendEscalationSMS(currentMisses: Int, maxMisses: Int) {
        try {
            val userPrefs = UserPreferences(this)
            val caregiverPhone = userPrefs.getCaregiverPhone()
            val elderName = userPrefs.getUserName()

            if (caregiverPhone.isEmpty()) {
                Log.e(TAG, "Cannot send escalation SMS - caregiver phone not set")
                return
            }

            // Include last known location if available
            val loc = com.oppam.oppamlauncher.status.LocationStatus(this).get()
            val locText = if (loc != null) {
                " Last location: ${loc.lat}, ${loc.lng} (±${loc.accuracy}m)."
            } else ""

            val sms = "🚨 Escalation: $elderName missed $currentMisses/$maxMisses reminders for: $message.$locText"
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(caregiverPhone, null, sms, null, null)
            Log.d(TAG, "Escalation SMS sent to caregiver: $caregiverPhone")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send escalation SMS", e)
        }
    }
    
    private fun stopAlarm() {
        Log.d(TAG, "Stopping alarm, vibration, and TTS")
        
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        
        vibrator?.cancel()
        
        tts?.stop()
        tts?.shutdown()
        tts = null
        
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "Wake lock released")
            }
        }
        wakeLock = null
        
        // Cancel any pending handlers
        noResponseTimeoutRunnable?.let { handler.removeCallbacks(it) }
        reRingRunnable?.let { handler.removeCallbacks(it) }
    }
    
    override fun onDestroy() {
        Log.d(TAG, "AlarmActivity destroyed")
        super.onDestroy()
        stopAlarm()
    }
}

@Composable
fun AlarmVerificationScreen(
    message: String,
    showButtons: Boolean,
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD32F2F)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Pulsing alarm icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("⏰", fontSize = 64.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "അപ്പച്ചാ!",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Message box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Text(
                    message,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 42.sp
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Verification buttons (shown after 10-second delay)
            if (showButtons) {
                Text(
                    "മരുന്ന് കഴിച്ചോ?", // "Did you take medicine?"
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // YES button (Green, large, elderly-friendly)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(130.dp)
                        .background(Color(0xFF4CAF50), RoundedCornerShape(20.dp))
                        .clickable { onYes() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "✓",
                            fontSize = 48.sp,
                            color = Color.White
                        )
                        Text(
                            "അതെ", // Yes
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // NO button (Orange, large, elderly-friendly)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(130.dp)
                        .background(Color(0xFFFF9800), RoundedCornerShape(20.dp))
                        .clickable { onNo() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "✗",
                            fontSize = 48.sp,
                            color = Color.White
                        )
                        Text(
                            "ഇല്ല", // No
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                // Show waiting message while TTS sequence plays
                Text(
                    "ശ്രദ്ധിക്കൂ...", // "Listen..."
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}