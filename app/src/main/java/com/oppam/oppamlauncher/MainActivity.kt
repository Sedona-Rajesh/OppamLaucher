package com.oppam.oppamlauncher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.oppam.oppamlauncher.ui.theme.OppamLauncherTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tts = TextToSpeech(this) {
            tts.language = Locale("ml", "IN")
        }

        setContent {
            OppamLauncherTheme {
                OppamApp()
            }
        }
    }
}

/* ---------------- ROOT APP ---------------- */

@Composable
fun OppamApp() {
    var role by remember { mutableStateOf("ELDER") } // ELDER / CAREGIVER
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

    // Reminder trigger loop (demo: triggers immediately)
    LaunchedEffect(reminders.size) {
        if (reminders.isNotEmpty()) {
            activeReminder = reminders.last()
            speakMalayalam(activeReminder!!.messageMl)

            // simulate 10 minutes as 10 seconds
            delay(10_000)
            showConfirm = true
        }
    }

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
                    "HOME" -> ElderHome(
                        onFamily = { elderScreen = "FAMILY" },
                        onCaregiver = { role = "CAREGIVER" }
                    )

                    "FAMILY" -> ElderFamilyScreen(
                        family = family,
                        onBack = { elderScreen = "HOME" },
                        onAdd = { elderScreen = "ADD_FAMILY" }
                    )

                    "ADD_FAMILY" -> AddFamilyScreen(
                        onSave = { n, p ->
                            family.add(FamilyMember(n, p))
                            elderScreen = "FAMILY"
                        },
                        onBack = { elderScreen = "FAMILY" }
                    )
                }
            }
        }

        "CAREGIVER" -> CaregiverDashboard(
            alerts = alerts,
            onAddReminder = {
                reminders.add(
                    Reminder(
                        messageMl = "അപ്പച്ചാ, മരുന്ന് എടുത്തോളൂ",
                        messageEn = "Medicine reminder"
                    )
                )
            },
            onBack = { role = "ELDER" }
        )
    }
}

/* ---------------- ELDER HOME (MALAYALAM) ---------------- */

@Composable
fun ElderHome(
    onFamily: () -> Unit,
    onCaregiver: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F4))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        Text("ഒപ്പം", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("ഞാൻ ഒപ്പമുണ്ട്", fontSize = 20.sp, color = Color.DarkGray)

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
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCaregiver() },
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
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

/* ---------------- CAREGIVER ---------------- */

@Composable
fun CaregiverDashboard(
    alerts: List<Alert>,
    onAddReminder: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

        Text("Caregiver Dashboard", fontSize = 26.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(12.dp))

        ElderButton("Set Medicine Reminder", Color(0xFF1976D2), onAddReminder)

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

/* ---------------- COMMON UI ---------------- */

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
