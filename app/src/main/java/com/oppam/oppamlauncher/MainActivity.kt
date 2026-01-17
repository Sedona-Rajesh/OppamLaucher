package com.oppam.oppamlauncher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.app.AlarmManager
import android.provider.Settings
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oppam.oppamlauncher.emergency.EmergencyCallPreferences
import com.oppam.oppamlauncher.emergency.EmergencyCallIntegration
import com.oppam.oppamlauncher.emergency.ui.EmergencyCallConsentScreen
import com.oppam.oppamlauncher.status.StatusTracker
import com.oppam.oppamlauncher.ui.theme.OppamLauncherTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// 🆕 Auth/Login Imports
import com.oppam.oppamlauncher.auth.*

/* ---------------- DATA MODELS ---------------- */

data class FamilyMember(val name: String, val phone: String)
data class Alert(val time: String, val message: String)
data class Reminder(
    val messageMl: String,
    val messageEn: String,
    var status: String = "Pending"
)

/* ---------------- GLOBAL TTS ---------------- */

lateinit var tts: TextToSpeech

fun speakMalayalam(text: String) {
    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
}

/* ---------------- ACTIVITY ---------------- */

class MainActivity : ComponentActivity() {
    private val requiredPermissions = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    private val multiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            // Log results; proceed to exact alarm request if needed
            requestExactAlarmIfNeeded()
        }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tts = TextToSpeech(this) {
            tts.language = Locale("ml", "IN")
        }

        setContent {
            OppamLauncherTheme {
                // Request runtime permissions early
                LaunchedEffect(Unit) {
                    requestRuntimePermissions()
                    requestExactAlarmIfNeeded()
                }
                OppamApp()
            }
        }
    }
    
    private fun requestRuntimePermissions() {
        val toRequest = requiredPermissions.filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            multiplePermissionsLauncher.launch(toRequest.toTypedArray())
        }
    }
    
    private fun requestExactAlarmIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(AlarmManager::class.java)
            val canExact = am.canScheduleExactAlarms()
            if (!canExact) {
                // Direct user to "Alarms & reminders" special app access
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                    Toast.makeText(
                        this,
                        "Please allow 'Alarms & reminders' for Oppam",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    // Fallback: open special app access screen
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:" + packageName)
                    }
                    startActivity(intent)
                }
            }
        }
    }
}

/* ---------------- ROOT APP ---------------- */

@Composable
fun OppamApp() {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val emergencyPrefs = remember { EmergencyCallPreferences(context) }
    var isLoggedIn by remember { mutableStateOf(userPrefs.isLoggedIn()) }
    var showEmergencyConsent by remember { mutableStateOf(false) }
    
    // 🆕 Check if user is logged in
    if (!isLoggedIn) {
        LoginScreen(
            onLoginComplete = { userType ->
                isLoggedIn = true
                // Show emergency consent only for ELDER users who haven't consented
                if (userType == "ELDER" && !emergencyPrefs.hasConsentGiven()) {
                    showEmergencyConsent = true
                }
            }
        )
        return
    }
    
    // 🆕 Emergency consent screen for ELDER users
    if (showEmergencyConsent) {
        EmergencyCallConsentScreen(
            onConsent = {
                // Set up emergency monitoring with caregiver phone
                val caregiverPhone = userPrefs.getCaregiverPhone()
                if (caregiverPhone.isNotBlank()) {
                    emergencyPrefs.setEmergencyContact(caregiverPhone)
                }
                EmergencyCallIntegration.initializeIfEnabled(context)
                showEmergencyConsent = false
            },
            onDecline = {
                showEmergencyConsent = false
            }
        )
        return
    }
    
    // Get user type - NO SWITCHING between roles
    val userType = remember { userPrefs.getUserType() }
    
    // Logout function
    val onLogout = {
        userPrefs.clearAllData()
        isLoggedIn = false
    }
    
    // Route to appropriate experience based on user type
    when (userType) {
        "ELDER" -> ElderExperience(onLogout = onLogout)
        "CAREGIVER" -> CaregiverExperience(
            elderName = userPrefs.getLinkedElderName().ifBlank { "വയോധികൻ" },
            elderPhone = userPrefs.getLinkedElderPhone(),
            onLogout = onLogout
        )
    }
}

/* ==================== ELDER EXPERIENCE ==================== */

@Composable
fun ElderExperience(onLogout: () -> Unit) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    var currentScreen by remember { mutableStateOf("HOME") }
    
    // Start AI companion service
    LaunchedEffect(Unit) {
        context.startService(Intent(context, com.oppam.oppamlauncher.services.CompanionService::class.java))
    }
    
    val family = remember {
        mutableStateListOf(
            FamilyMember(userPrefs.getCaregiverName().ifBlank { "മകൻ" }, userPrefs.getCaregiverPhone())
        )
    }
    
    when (currentScreen) {
        "HOME" -> ElderHome(
            userName = userPrefs.getUserName(),
            onFamily = { currentScreen = "FAMILY" },
            onLogout = onLogout
        )
        "FAMILY" -> ElderFamilyScreen(
            family = family,
            onBack = { currentScreen = "HOME" },
            onAdd = { currentScreen = "ADD_FAMILY" }
        )
        "ADD_FAMILY" -> AddFamilyScreen(
            onSave = { n, p ->
                family.add(FamilyMember(n, p))
                currentScreen = "FAMILY"
            },
            onBack = { currentScreen = "FAMILY" }
        )
    }
}

/* ---------------- ELDER HOME (LARGE UI FOR ELDERLY) ---------------- */

@Composable
fun ElderHome(
    userName: String,
    onFamily: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val alarmStorage = remember { com.oppam.oppamlauncher.alarm.AlarmStorage(context) }
    val testAlarmScheduler = remember { com.oppam.oppamlauncher.alarm.AlarmScheduler }
    
    // Auto-refresh alarm status
    var refreshTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(refreshTrigger) {
        delay(1000)
        alarmStorage.markMissedAlarms()
        refreshTrigger++
    }
    
    val upcomingAlarms = remember(refreshTrigger) { alarmStorage.getUpcomingAlarms() }
    val missedAlarms = remember(refreshTrigger) { alarmStorage.getAlarmsByStatus("missed") }
    val snoozedAlarms = remember(refreshTrigger) { alarmStorage.getAlarmsByStatus("snoozed") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header with user name
        Column {
            Text(
                "ഒപ്പം",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Text(
                if (userName.isNotBlank()) "നമസ്കാരം, $userName" else "ഞാൻ ഒപ്പമുണ്ട്",
                fontSize = 24.sp,
                color = Color(0xFF5F6368)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Quick local test: schedule a 60-second alarm to verify device behavior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFBBDEFB), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "പരീക്ഷണം: 60 സെക്കൻഡ് കഴിഞ്ഞ് അലാറം", // Test: Alarm in 60 seconds
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D47A1)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val alarmId = alarmStorage.generateAlarmId()
                        val message = "മരുന്ന് കഴിക്കൂ"
                        val t = System.currentTimeMillis() + 60_000
                        com.oppam.oppamlauncher.alarm.AlarmScheduler.scheduleAlarm(
                            context,
                            alarmId,
                            message,
                            t
                        )
                        Toast.makeText(context, "60s alarm scheduled", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("ഇപ്പോൾ പരീക്ഷിക്കുക") // Try now
                }
            }
        }
        
        // Alarm status bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AlarmStatusCard(
                count = upcomingAlarms.size,
                label = "Scheduled",
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            AlarmStatusCard(
                count = missedAlarms.size,
                label = "Missed",
                color = Color(0xFFFF5722),
                modifier = Modifier.weight(1f)
            )
            AlarmStatusCard(
                count = snoozedAlarms.size,
                label = "Snoozed",
                color = Color(0xFFFFC107),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        // Location sharing controls (consent-based, manual)
        com.oppam.oppamlauncher.ui.elder.LocationSharingControls()

        // Large action buttons
        LargeElderButton(
            text = "🔔\nഅറിയിപ്പുകൾ", // Notifications
            color = Color(0xFFFFA000),
            onClick = { /* TODO: Navigate to notifications screen */ }
        )
        
        LargeElderButton(
            text = "📞\nകുടുംബത്തെ വിളിക്കുക",
            color = Color(0xFF34A853),
            onClick = onFamily
        )

        LargeElderButton(
            text = "📱\nഫോൺ തുറക്കുക",
            color = Color(0xFF1976D2),
            onClick = {
                context.startActivity(Intent(Intent.ACTION_DIAL))
            }
        )
        
        LargeElderButton(
            text = "📸\nഫോട്ടോകൾ കാണുക",
            color = Color(0xFFE91E63),
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW).apply { type = "image/*" })
            }
        )

        Spacer(modifier = Modifier.weight(1f))
        
        // Logout button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                .clickable { onLogout() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "പുറത്തുകടക്കുക (Logout)",
                fontSize = 18.sp,
                color = Color(0xFF5F6368)
            )
        }
    }
}

@Composable
fun AlarmStatusCard(count: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "$count",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                fontSize = 12.sp,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

/* ---------------- FAMILY ---------------- */

@Composable
fun ElderFamilyScreen(
    family: List<FamilyMember>,
    onBack: () -> Unit,
    onAdd: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

        Text("കുടുംബാംഗങ്ങൾ", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(12.dp))

        family.forEach {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${it.phone}"))
                        )
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(it.name, fontSize = 20.sp)
                    Text(it.phone, fontSize = 16.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ElderButton("➕ പുതിയ അംഗം ചേർക്കുക", Color(0xFF1976D2), onAdd)
        ElderButton("തിരികെ", Color.DarkGray, onBack)
    }
}

/* ---------------- ADD FAMILY ---------------- */

@Composable
fun AddFamilyScreen(
    onSave: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

        Text("പുതിയ കുടുംബാംഗം", fontSize = 26.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("പേര്") })
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("ഫോൺ നമ്പർ") })

        Spacer(modifier = Modifier.height(16.dp))

        ElderButton("സേവ് ചെയ്യുക", Color(0xFF388E3C)) {
            if (name.isNotBlank() && phone.isNotBlank()) onSave(name, phone)
        }
        ElderButton("റദ്ദാക്കുക", Color.DarkGray, onBack)
    }
}

/* ---------------- CONFIRMATION ---------------- */

@Composable
fun ElderConfirmationScreen(
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            "അപ്പച്ചാ, മരുന്ന് എടുത്തോ?",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            RoundButton("അതെ", Color(0xFF388E3C), onYes)
            RoundButton("ഇല്ല", Color(0xFFD32F2F), onNo)
        }
    }
}

/* ==================== CAREGIVER EXPERIENCE ==================== */

@Composable
fun CaregiverExperience(
    elderName: String,
    elderPhone: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf("DASHBOARD") }
    val alerts = remember { mutableStateListOf<Alert>() }
    
    when (currentScreen) {
        "DASHBOARD" -> CaregiverDashboard(
            elderName = elderName,
            elderPhone = elderPhone,
            onSetReminder = { currentScreen = "REMINDERS" },
            onLogout = onLogout
        )
        "REMINDERS" -> com.oppam.oppamlauncher.reminders.ui.CaregiverReminderManager(
            onBack = { currentScreen = "DASHBOARD" }
        )
        "SETTINGS" -> CaregiverSettings(
            onBack = { currentScreen = "DASHBOARD" }
        )
    }
}

/* ---------------- CAREGIVER DASHBOARD ---------------- */

@Composable
fun CaregiverDashboard(
    elderName: String,
    elderPhone: String,
    onSetReminder: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val statusTracker = remember { StatusTracker(context) }
    // Online/offline SMS polling disabled per requirement.
    var isElderOnline by remember { mutableStateOf(false) }
    var lastSeenText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            "Caregiver Dashboard",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )
        Text(
            "Monitoring: $elderName",
            fontSize = 20.sp,
            color = Color(0xFF5F6368)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Elder status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(elderName, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text(elderPhone, fontSize = 16.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        if (isElderOnline) Color(0xFF34A853) else Color.Gray,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isElderOnline) "Online" else "Offline",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isElderOnline) Color(0xFF34A853) else Color.Gray
                            )
                        }
                    }
                    
                    // Quick call button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF34A853), CircleShape)
                            .clickable {
                                context.startActivity(
                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$elderPhone"))
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📞", fontSize = 28.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Live location (if elder shared)
        com.oppam.oppamlauncher.ui.caregiver.LocationDashboard(elderName = elderName)
        Spacer(modifier = Modifier.height(24.dp))
        
        // Alerts section
        Text(
            "Recent Alerts",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Dummy data for now
        val alerts = listOf<Alert>()

        if (alerts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✓", fontSize = 48.sp, color = Color(0xFF34A853))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No alerts - Everything is fine",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            alerts.forEach { alert ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(alert.message, fontSize = 16.sp)
                            Text(alert.time, fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Send Reminder button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFFE91E63), RoundedCornerShape(12.dp))
                .clickable { onSetReminder() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "⏰ Send Reminder to Elder",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Settings button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF1976D2), RoundedCornerShape(12.dp))
                .clickable { /* TODO: Navigate to settings */ },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "⚙️ Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Logout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                .clickable { onLogout() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Logout",
                fontSize = 18.sp,
                color = Color(0xFF5F6368)
            )
        }
    }
}

/* ---------------- CAREGIVER SETTINGS ---------------- */

@Composable
fun CaregiverSettings(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .padding(24.dp)
    ) {
        Text(
            "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Feature coming soon: Configure emergency alerts, notifications, and monitoring preferences.",
            fontSize = 16.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF1976D2), RoundedCornerShape(12.dp))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "← Back to Dashboard",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

/* ---------------- COMMON UI COMPONENTS ---------------- */

// Large button for elderly users - extra height and font size
@Composable
fun LargeElderButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(color, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )
    }
}

@Composable
fun ElderButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(color, RoundedCornerShape(18.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun RoundButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(color, RoundedCornerShape(60.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 22.sp, color = Color.White)
    }
}

@Composable
fun AppRow() {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AppTile("YouTube") { launchPackage(context, "com.google.android.youtube") }
        AppTile("WhatsApp") { launchPackage(context, "com.whatsapp") }
        AppTile("Phone") { context.startActivity(Intent(Intent.ACTION_DIAL)) }
        AppTile("Photos") {
            context.startActivity(Intent(Intent.ACTION_VIEW).apply { type = "image/*" })
        }
    }
}

@Composable
fun AppTile(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

/* ---------------- UTILS ---------------- */

fun launchPackage(context: Context, pkg: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
    if (intent != null) context.startActivity(intent)
    else Toast.makeText(context, "ആപ്പ് കണ്ടെത്താനായില്ല", Toast.LENGTH_SHORT).show()
}

fun timeNow(): String =
    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
