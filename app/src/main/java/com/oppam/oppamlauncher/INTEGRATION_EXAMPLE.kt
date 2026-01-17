package com.oppam.oppamlauncher

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * EMERGENCY CALL FEATURE - INTEGRATION EXAMPLE
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This file shows HOW to integrate the emergency call monitoring feature
 * into MainActivity.kt WITHOUT modifying existing code.
 * 
 * INTEGRATION APPROACH:
 * - Add a new screen state for emergency consent
 * - Add a settings option in caregiver dashboard
 * - Initialize service when app starts (if enabled)
 * - All existing functionality remains unchanged
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 */

/**
 * STEP 1: Add these imports to MainActivity.kt
 * (Add AFTER existing imports)
 */
/*
import com.oppam.oppamlauncher.emergency.*
import com.oppam.oppamlauncher.emergency.ui.*
*/

/**
 * STEP 2: Add emergency feature state to OppamApp()
 * (Add AFTER existing state variables)
 */
/*
@Composable
fun OppamApp() {
    var role by remember { mutableStateOf("ELDER") }
    var elderScreen by remember { mutableStateOf("HOME") }
    
    // ... existing state variables ...
    
    // 🆕 EMERGENCY CALL FEATURE STATE
    val context = LocalContext.current
    val emergencyPrefs = remember { EmergencyCallPreferences(context) }
    var showEmergencyConsent by remember { 
        mutableStateOf(
            !emergencyPrefs.hasConsentGiven() && 
            !emergencyPrefs.hasDeclinedConsent()
        ) 
    }
    var showEmergencySettings by remember { mutableStateOf(false) }
    
    // Initialize emergency service if enabled
    LaunchedEffect(Unit) {
        EmergencyCallIntegration.initializeIfEnabled(context)
    }
    
    // ... rest of existing code ...
}
*/

/**
 * STEP 3: Add emergency consent screen to navigation flow
 * (Wrap existing navigation in a when statement)
 */
/*
@Composable
fun OppamApp() {
    // ... state variables from Step 2 ...
    
    when {
        // Show consent screen first if not given
        showEmergencyConsent -> {
            EmergencyCallConsentScreen(
                onConsent = {
                    EmergencyCallIntegration.initializeIfEnabled(context)
                    showEmergencyConsent = false
                },
                onDecline = {
                    emergencyPrefs.setDeclineConsent(true)
                    showEmergencyConsent = false
                }
            )
        }
        
        // Show emergency settings if navigated
        showEmergencySettings -> {
            EmergencyCallSettingsScreen(
                onBack = { showEmergencySettings = false }
            )
        }
        
        // EXISTING NAVIGATION (unchanged)
        else -> {
            when (role) {
                "ELDER" -> {
                    if (showConfirm && activeReminder != null) {
                        ElderConfirmationScreen(...)
                    } else {
                        when (elderScreen) {
                            "HOME" -> ElderHome(...)
                            "FAMILY" -> ElderFamilyScreen(...)
                            "ADD_FAMILY" -> AddFamilyScreen(...)
                        }
                    }
                }
                "CAREGIVER" -> CaregiverDashboard(...)
            }
        }
    }
}
*/

/**
 * STEP 4: Add emergency settings button to CaregiverDashboard
 * (Add AFTER existing buttons in CaregiverDashboard)
 */
/*
@Composable
fun CaregiverDashboard(
    alerts: List<Alert>,
    onAddReminder: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        
        Text("Caregiver Dashboard", ...)
        
        ElderButton("Set Medicine Reminder", ..., onAddReminder)
        
        // 🆕 EMERGENCY CALL SETTINGS BUTTON
        ElderButton("🚨 Emergency Call Settings", Color(0xFFFF6F00)) {
            // Set showEmergencySettings = true in parent composable
            // or navigate to emergency settings
        }
        
        // ... rest of existing code ...
    }
}
*/

/**
 * STEP 5: (Optional) Show active monitoring indicator in ElderHome
 * (Add AFTER the "ഒപ്പം" header)
 */
/*
@Composable
fun ElderHome(
    onFamily: () -> Unit,
    onCaregiver: () -> Unit
) {
    val context = LocalContext.current
    val emergencyPrefs = remember { EmergencyCallPreferences(context) }
    
    Column(...) {
        Text("ഒപ്പം", ...)
        Text("ഞാൻ ഒപ്പമുണ്ട്", ...)
        
        // 🆕 SHOW MONITORING INDICATOR IF ACTIVE
        if (emergencyPrefs.isFeatureEnabled()) {
            ActiveMonitoringIndicator()
        }
        
        // ... rest of existing code ...
    }
}
*/

/**
 * STEP 6: Clean up on app destroy
 * (Add in MainActivity.onDestroy())
 */
/*
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // ... existing onCreate code ...
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 🆕 STOP EMERGENCY MONITORING
        EmergencyCallIntegration.stopMonitoring()
        
        // Clean up TTS
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}
*/

/**
 * COMPLETE INTEGRATION EXAMPLE
 * ════════════════════════════════════════════════════════════════════════
 * 
 * Here's what the complete OppamApp function would look like with
 * emergency call feature integrated:
 */
/*
@Composable
fun OppamApp() {
    var role by remember { mutableStateOf("ELDER") }
    var elderScreen by remember { mutableStateOf("HOME") }
    
    val alerts = remember { mutableStateListOf<Alert>() }
    val family = remember {
        mutableStateListOf(
            FamilyMember("മകൻ", "9876543210"),
            FamilyMember("മകൾ", "9123456789")
        )
    }
    val reminders = remember { mutableStateListOf<Reminder>() }
    var activeReminder by remember { mutableStateOf<Reminder?>(null) }
    var showConfirm by remember { mutableStateOf(false) }
    
    // 🆕 EMERGENCY CALL FEATURE STATE
    val context = LocalContext.current
    val emergencyPrefs = remember { EmergencyCallPreferences(context) }
    var showEmergencyConsent by remember { 
        mutableStateOf(
            !emergencyPrefs.hasConsentGiven() && 
            !emergencyPrefs.hasDeclinedConsent()
        ) 
    }
    var showEmergencySettings by remember { mutableStateOf(false) }
    
    // Initialize emergency service if enabled
    LaunchedEffect(Unit) {
        EmergencyCallIntegration.initializeIfEnabled(context)
        
        // Sync emergency contacts from family list
        if (emergencyPrefs.isFeatureEnabled() && family.isNotEmpty()) {
            val contacts = family.joinToString(",") { it.phone }
            emergencyPrefs.setEmergencyContacts(contacts)
        }
    }
    
    // Existing reminder trigger loop
    LaunchedEffect(reminders.size) {
        if (reminders.isNotEmpty()) {
            activeReminder = reminders.last()
            speakMalayalam(activeReminder!!.messageMl)
            delay(10_000)
            showConfirm = true
        }
    }
    
    when {
        // Emergency consent screen (only shows first time)
        showEmergencyConsent -> {
            EmergencyCallConsentScreen(
                onConsent = {
                    // Sync emergency contacts from family list
                    val contacts = family.joinToString(",") { it.phone }
                    emergencyPrefs.setEmergencyContacts(contacts)
                    
                    EmergencyCallIntegration.initializeIfEnabled(context)
                    showEmergencyConsent = false
                },
                onDecline = {
                    emergencyPrefs.setDeclineConsent(true)
                    showEmergencyConsent = false
                }
            )
        }
        
        // Emergency settings screen
        showEmergencySettings -> {
            EmergencyCallSettingsScreen(
                onBack = { showEmergencySettings = false }
            )
        }
        
        // EXISTING APP NAVIGATION (unchanged)
        else -> {
            when (role) {
                "ELDER" -> {
                    if (showConfirm && activeReminder != null) {
                        ElderConfirmationScreen(
                            onYes = {
                                alerts.add(Alert(timeNow(), "${activeReminder!!.messageEn} - YES"))
                                activeReminder!!.status = "Taken"
                                showConfirm = false
                            },
                            onNo = {
                                alerts.add(Alert(timeNow(), "${activeReminder!!.messageEn} - NO"))
                                activeReminder!!.status = "Not Taken"
                                showConfirm = false
                            }
                        )
                    } else {
                        when (elderScreen) {
                            "HOME" -> ElderHomeWithEmergency(
                                onFamily = { elderScreen = "FAMILY" },
                                onCaregiver = { role = "CAREGIVER" },
                                emergencyEnabled = emergencyPrefs.isFeatureEnabled()
                            )
                            "FAMILY" -> ElderFamilyScreen(
                                family = family,
                                onBack = { elderScreen = "HOME" },
                                onAdd = { elderScreen = "ADD_FAMILY" }
                            )
                            "ADD_FAMILY" -> AddFamilyScreen(
                                onSave = { n, p ->
                                    family.add(FamilyMember(n, p))
                                    
                                    // 🆕 Sync with emergency contacts
                                    if (emergencyPrefs.isFeatureEnabled()) {
                                        emergencyPrefs.addEmergencyContact(p)
                                    }
                                    
                                    elderScreen = "FAMILY"
                                },
                                onBack = { elderScreen = "FAMILY" }
                            )
                        }
                    }
                }
                
                "CAREGIVER" -> CaregiverDashboardWithEmergency(
                    alerts = alerts,
                    onAddReminder = {
                        reminders.add(
                            Reminder(
                                messageMl = "അപ്പച്ചാ, മരുന്ന് എടുത്തോളൂ",
                                messageEn = "Medicine reminder"
                            )
                        )
                    },
                    onEmergencySettings = { showEmergencySettings = true },
                    onBack = { role = "ELDER" }
                )
            }
        }
    }
}

// Enhanced ElderHome with emergency indicator
@Composable
fun ElderHomeWithEmergency(
    onFamily: () -> Unit,
    onCaregiver: () -> Unit,
    emergencyEnabled: Boolean
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF4F4F4)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("ഒപ്പം", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("ഞാൻ ഒപ്പമുണ്ട്", fontSize = 20.sp, color = Color.DarkGray)
        
        // 🆕 Emergency monitoring indicator
        if (emergencyEnabled) {
            ActiveMonitoringIndicator()
        }
        
        // Rest of existing ElderHome content...
        ElderButton("📞 കുടുംബത്തെ വിളിക്കുക", Color(0xFF388E3C), onFamily)
        ElderButton("🩺 ആരോഗ്യ കുറിപ്പ്", Color(0xFF1976D2)) {
            Toast.makeText(context, "ആരോഗ്യം രേഖപ്പെടുത്തി", Toast.LENGTH_SHORT).show()
        }
        Divider(thickness = 2.dp)
        Text("ആപ്പുകൾ", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        AppRow()
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "ഇവിടെ അമർത്തി കെയർഗിവർ മോഡ് തുറക്കുക",
            modifier = Modifier.fillMaxWidth().clickable { onCaregiver() },
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
    }
}

// Enhanced CaregiverDashboard with emergency settings
@Composable
fun CaregiverDashboardWithEmergency(
    alerts: List<Alert>,
    onAddReminder: () -> Unit,
    onEmergencySettings: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Caregiver Dashboard", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        ElderButton("Set Medicine Reminder", Color(0xFF1976D2), onAddReminder)
        
        // 🆕 Emergency settings button
        ElderButton("🚨 Emergency Call Settings", Color(0xFFFF6F00), onEmergencySettings)
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Alerts", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        
        if (alerts.isEmpty()) {
            Text("No alerts")
        } else {
            alerts.forEach {
                Text("• ${it.message} (${it.time})")
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        ElderButton("Back", Color.DarkGray, onBack)
    }
}
*/

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * THAT'S IT!
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * The emergency call feature is now integrated WITHOUT modifying any
 * existing functionality. Everything is additive and optional.
 * 
 * To test:
 * 1. Run the app
 * 2. You'll see the consent screen on first launch
 * 3. Accept consent and grant permissions
 * 4. Make a test call and say "സഹായം" (help)
 * 5. Check caregiver's phone for SMS alert
 * 
 * To disable:
 * 1. Go to Caregiver Dashboard
 * 2. Click "Emergency Call Settings"
 * 3. Toggle off
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 */
