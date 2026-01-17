package com.oppam.oppamlauncher.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ---------------- LOGIN/REGISTRATION SCREEN ---------------- */

@Composable
fun LoginScreen(
    onLoginComplete: (String) -> Unit // Returns user type: "ELDER" or "CAREGIVER"
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    
    var userType by remember { mutableStateOf("") } // "", "ELDER", "CAREGIVER"
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var caregiverName by remember { mutableStateOf("") }
    var caregiverPhone by remember { mutableStateOf("") }
    
    when {
        userType.isEmpty() -> {
            // Select user type
            UserTypeSelectionScreen(
                onSelectElder = { userType = "ELDER" },
                onSelectCaregiver = { userType = "CAREGIVER" }
            )
        }
        
        userType == "ELDER" -> {
            ElderRegistrationScreen(
                name = name,
                phone = phone,
                caregiverName = caregiverName,
                caregiverPhone = caregiverPhone,
                onNameChange = { name = it },
                onPhoneChange = { phone = it },
                onCaregiverNameChange = { caregiverName = it },
                onCaregiverPhoneChange = { caregiverPhone = it },
                onRegister = {
                    if (name.isNotBlank() && phone.isNotBlank() && caregiverPhone.isNotBlank()) {
                        userPrefs.registerUser(
                            name = name,
                            phone = phone,
                            userType = "ELDER",
                            caregiverPhone = caregiverPhone,
                            caregiverName = caregiverName.ifBlank { "മകൻ" }
                        )
                        onLoginComplete("ELDER")
                    }
                },
                onBack = { userType = "" }
            )
        }
        
        userType == "CAREGIVER" -> {
            CaregiverRegistrationScreen(
                name = name,
                phone = phone,
                elderPhone = caregiverPhone,
                onNameChange = { name = it },
                onPhoneChange = { phone = it },
                onElderPhoneChange = { caregiverPhone = it },
                onRegister = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        userPrefs.registerUser(
                            name = name,
                            phone = phone,
                            userType = "CAREGIVER"
                        )
                        
                        if (caregiverPhone.isNotBlank()) {
                            userPrefs.setLinkedElderPhone(caregiverPhone)
                        }
                        
                        onLoginComplete("CAREGIVER")
                    }
                },
                onBack = { userType = "" }
            )
        }
    }
}

/* ---------------- USER TYPE SELECTION ---------------- */

@Composable
fun UserTypeSelectionScreen(
    onSelectElder: () -> Unit,
    onSelectCaregiver: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF1976D2), Color(0xFF64B5F6))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // App logo/title
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("👥", fontSize = 60.sp)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "ഒപ്പം",
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                "Oppam Launcher",
                fontSize = 22.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(56.dp))
            
            Text(
                "നിങ്ങൾ ആരാണ്?",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            
            Text(
                "Select Your Role",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Elder button - large and prominent
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                onClick = onSelectElder,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("👴", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "വയോധികൻ",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    Text(
                        "Elder",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Caregiver button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                onClick = onSelectCaregiver,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("👨‍⚕️", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "പരിചരിക്കുന്നവർ",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34A853)
                    )
                    Text(
                        "Caregiver",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/* ---------------- ELDER REGISTRATION SCREEN ---------------- */

@Composable
fun ElderRegistrationScreen(
    name: String,
    phone: String,
    caregiverName: String,
    caregiverPhone: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onCaregiverNameChange: (String) -> Unit,
    onCaregiverPhoneChange: (String) -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(24.dp)
    ) {
        
        IconButton(onClick = onBack) {
            Text("← തിരികെ", fontSize = 18.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "മുതിർന്ന വ്യക്തി രജിസ്ട്രേഷൻ",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            "Elder Registration",
            fontSize = 16.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("നിങ്ങളുടെ പേര് / Your Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("ഫോൺ നമ്പർ / Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "പരിചരണം നൽകുന്നയാളുടെ വിവരങ്ങൾ",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            "Caregiver Details (who will receive alerts)",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = caregiverName,
            onValueChange = onCaregiverNameChange,
            label = { Text("കെയർഗിവർ പേര് / Name (മകൻ, മകൾ)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = caregiverPhone,
            onValueChange = onCaregiverPhoneChange,
            label = { Text("കെയർഗിവർ ഫോൺ / Phone *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onRegister,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(12.dp),
            enabled = name.isNotBlank() && phone.isNotBlank() && caregiverPhone.isNotBlank()
        ) {
            Text("രജിസ്റ്റർ ചെയ്യുക / Register", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/* ---------------- CAREGIVER REGISTRATION ---------------- */

@Composable
fun CaregiverRegistrationScreen(
    name: String,
    phone: String,
    elderPhone: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onElderPhoneChange: (String) -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(24.dp)
    ) {
        
        IconButton(onClick = onBack) {
            Text("← Back", fontSize = 18.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Caregiver Registration",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            "രജിസ്ട്രേഷൻ",
            fontSize = 16.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Your Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Link to Elder (Optional)",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            "Enter elder's phone to monitor their alerts",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = elderPhone,
            onValueChange = onElderPhoneChange,
            label = { Text("Elder's Phone Number (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onRegister,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            shape = RoundedCornerShape(12.dp),
            enabled = name.isNotBlank() && phone.isNotBlank()
        ) {
            Text("Register", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
