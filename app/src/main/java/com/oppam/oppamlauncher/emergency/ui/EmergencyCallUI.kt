package com.oppam.oppamlauncher.emergency.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.oppam.oppamlauncher.emergency.EmergencyCallPreferences

/* ---------------- CONSENT SCREEN ---------------- */

@Composable
fun EmergencyCallConsentScreen(
    onConsent: () -> Unit,
    onDecline: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { EmergencyCallPreferences(context) }
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            prefs.setConsentGiven(true)
            prefs.setFeatureEnabled(true)
            onConsent()
        } else {
            showPermissionDialog = true
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        
        // Icon/Header
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFFE3F2FD), CircleShape)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Text("🚨", fontSize = 40.sp)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "എമർജൻസി കോൾ സംരക്ഷണം",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            "Emergency Call Protection",
            fontSize = 18.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Privacy-focused explanation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ഈ സവിശേഷത എങ്ങനെ പ്രവർത്തിക്കുന്നു:", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                
                PrivacyPoint("✓", "കോളുകൾ സമയത്ത് അപകട സൂചനകൾ കണ്ടെത്തുന്നു")
                PrivacyPoint("✓", "വേദന/സഹായം എന്നീ വാക്കുകൾ തിരിച്ചറിയുന്നു")
                PrivacyPoint("✓", "കെയർഗിവർക്ക് അലേർട്ട് അയയ്ക്കുന്നു")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("🔒 സ്വകാര്യത ഉറപ്പ്:", fontWeight = FontWeight.SemiBold, color = Color(0xFF1976D2))
                Spacer(modifier = Modifier.height(8.dp))
                
                PrivacyPoint("•", "ഓഡിയോ റെക്കോർഡ് ചെയ്യില്ല")
                PrivacyPoint("•", "സംഭാഷണം സൂക്ഷിക്കില്ല")
                PrivacyPoint("•", "തത്സമയ വിശകലനം മാത്രം")
                PrivacyPoint("•", "നിങ്ങൾക്ക് എപ്പോൾ വേണമെങ്കിലും നിർത്താം")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Consent buttons
        Button(
            onClick = {
                val permissions = arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.RECORD_AUDIO
                )
                
                val allGranted = permissions.all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }
                
                if (allGranted) {
                    prefs.setConsentGiven(true)
                    prefs.setFeatureEnabled(true)
                    onConsent()
                } else {
                    permissionLauncher.launch(permissions)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("സമ്മതിക്കുന്നു & സജീവമാക്കുക", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = onDecline,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ഇപ്പോൾ വേണ്ട", fontSize = 18.sp)
        }
    }
    
    // Permission explanation dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("അനുമതികൾ ആവശ്യമാണ്") },
            text = { 
                Text("എമർജൻസി കോൾ മോണിറ്ററിങ്ങിന് ഫോൺ സ്റ്റേറ്റും ഓഡിയോ അനുമതികളും ആവശ്യമാണ്. നിങ്ങൾക്ക് സെറ്റിംഗുകളിൽ നിന്ന് അനുമതികൾ നൽകാം അല്ലെങ്കിൽ വീണ്ടും ശ്രമിക്കുക.") 
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    // Try requesting permissions again
                    val permissions = arrayOf(
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.RECORD_AUDIO
                    )
                    permissionLauncher.launch(permissions)
                }) {
                    Text("വീണ്ടും ശ്രമിക്കുക", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    onDecline()
                }) {
                    Text("ഇപ്പോൾ വേണ്ട")
                }
            }
        )
    }
}

@Composable
private fun PrivacyPoint(bullet: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(bullet, modifier = Modifier.width(24.dp))
        Text(text, fontSize = 14.sp)
    }
}

/* ---------------- EMERGENCY PANIC BUTTON OVERLAY ---------------- */

@Composable
fun EmergencyPanicOverlay(
    onPanicPressed: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                
                Text(
                    "അടിയന്തിരാവസ്ഥയാണോ?",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Large panic button
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(Color(0xFFD32F2F), CircleShape)
                        .border(6.dp, Color.White, CircleShape)
                        .clickable { onPanicPressed() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🆘", fontSize = 50.sp)
                        Text(
                            "SOS",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                Text(
                    "കെയർഗിവറിന് അലേർട്ട് അയയ്ക്കും",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("റദ്ദാക്കുക")
                }
            }
        }
    }
}

/* ---------------- EMERGENCY SETTINGS SCREEN ---------------- */

@Composable
fun EmergencyCallSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { EmergencyCallPreferences(context) }
    
    var isEnabled by remember { mutableStateOf(prefs.isFeatureEnabled()) }
    var showDisableConfirm by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(20.dp)
    ) {
        
        Text(
            "Emergency Call Settings",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Emergency Monitoring",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Detect distress during calls",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { newValue ->
                            if (!newValue) {
                                showDisableConfirm = true
                            } else {
                                isEnabled = true
                                prefs.setFeatureEnabled(true)
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                Divider()
                Spacer(modifier = Modifier.height(20.dp))
                
                // Feature details
                SettingItem(
                    icon = "🔊",
                    title = "Voice Analysis",
                    description = "Real-time distress detection"
                )
                
                SettingItem(
                    icon = "🔑",
                    title = "Keyword Detection",
                    description = "സഹായം, HELP, EMERGENCY, വേദന"
                )
                
                SettingItem(
                    icon = "🚨",
                    title = "Panic Button",
                    description = "Manual emergency alert during calls"
                )
                
                SettingItem(
                    icon = "📱",
                    title = "Caregiver Alert",
                    description = "Instant notification to caregiver"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Privacy reminder
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                Text("🔒", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Privacy Protected",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        "No audio is recorded or stored. Real-time analysis only.",
                        fontSize = 13.sp,
                        color = Color(0xFF558B2F)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Back", fontSize = 16.sp)
        }
    }
    
    // Disable confirmation dialog
    if (showDisableConfirm) {
        AlertDialog(
            onDismissRequest = { showDisableConfirm = false },
            title = { Text("Disable Emergency Monitoring?") },
            text = { 
                Text("This will stop monitoring calls for distress. You can re-enable it anytime from settings.") 
            },
            confirmButton = {
                TextButton(onClick = {
                    isEnabled = false
                    prefs.setFeatureEnabled(false)
                    showDisableConfirm = false
                }) {
                    Text("Disable", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingItem(icon: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(icon, fontSize = 24.sp, modifier = Modifier.width(40.dp))
        Column {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(description, fontSize = 13.sp, color = Color.Gray)
        }
    }
}

/* ---------------- ACTIVE MONITORING INDICATOR ---------------- */

@Composable
fun ActiveMonitoringIndicator() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(0xFF4CAF50), CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Emergency monitoring active",
                fontSize = 13.sp,
                color = Color(0xFF1976D2)
            )
        }
    }
}
