package com.oppam.oppamlauncher.reminders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oppam.oppamlauncher.auth.UserPreferences
import com.oppam.oppamlauncher.reminders.Reminder
import com.oppam.oppamlauncher.reminders.ReminderPreferences
import java.text.SimpleDateFormat
import java.util.*

/* ==================== CAREGIVER REMINDER MANAGER ==================== */

@Composable
fun CaregiverReminderManager(onBack: () -> Unit) {
    val context = LocalContext.current
    val reminderPrefs = remember { ReminderPreferences(context) }
    var showAddDialog by remember { mutableStateOf(false) }
    var reminders by remember { mutableStateOf(reminderPrefs.getAllReminders()) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Set Reminders",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF1976D2), RoundedCornerShape(12.dp))
                    .clickable { showAddDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontSize = 28.sp, color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Reminders list
        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏰", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No reminders set",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reminders) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onDelete = {
                            reminderPrefs.deleteReminder(reminder.id)
                            reminders = reminderPrefs.getAllReminders()
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Text("← Back", fontSize = 18.sp, color = Color(0xFF5F6368))
        }
    }
    
    if (showAddDialog) {
        AddReminderDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { reminder ->
                reminderPrefs.saveReminder(reminder)
                reminders = reminderPrefs.getAllReminders()
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ReminderCard(reminder: Reminder, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reminder.titleMl,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    reminder.messageMl,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatTime(reminder.time),
                    fontSize = 12.sp,
                    color = Color(0xFF1976D2)
                )
            }
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Text("🗑", fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun AddReminderDialog(onDismiss: () -> Unit, onAdd: (Reminder) -> Unit) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val reminderPrefs = remember { ReminderPreferences(context) }
    val alarmStorage = remember { com.oppam.oppamlauncher.alarm.AlarmStorage(context) }
    
    var titleMl by remember { mutableStateOf("") }
    var messageMl by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf("") }
    var selectedHour by remember { mutableStateOf(8) }
    var selectedMinute by remember { mutableStateOf(0) }
    var sendNow by remember { mutableStateOf(true) }
    
    val templates = listOf(
        "മരുന്ന് കഴിക്കുക" to "മരുന്ന് എടുക്കാൻ സമയമായി",
        "ഭക്ഷണം കഴിക്കുക" to "ഭക്ഷണം കഴിക്കൂ",
        "വെള്ളം കുടിക്കുക" to "വെള്ളം കുടിക്കാൻ മറക്കല്ലേ",
        "വ്യായാമം ചെയ്യുക" to "അല്പം നടന്നാലോ?",
        "ഉറങ്ങാൻ സമയം" to "ഉറങ്ങാൻ സമയമായി"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Set Alarm/Reminder", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Send Now vs Schedule Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sendNow,
                        onClick = { sendNow = true },
                        label = { Text("Send Now") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !sendNow,
                        onClick = { sendNow = false },
                        label = { Text("Schedule Alarm") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Time picker (only show when scheduling)
                if (!sendNow) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Alarm Time:", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Hour selector
                                Box(
                                    modifier = Modifier
                                        .size(80.dp, 60.dp)
                                        .background(Color.White, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("▲", modifier = Modifier.clickable { 
                                            selectedHour = (selectedHour + 1) % 24 
                                        })
                                        Text(String.format("%02d", if (selectedHour % 12 == 0) 12 else selectedHour % 12), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                        Text("▼", modifier = Modifier.clickable { 
                                            selectedHour = if (selectedHour == 0) 23 else selectedHour - 1 
                                        })
                                    }
                                }
                                
                                Text("  :  ", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                
                                // Minute selector
                                Box(
                                    modifier = Modifier
                                        .size(80.dp, 60.dp)
                                        .background(Color.White, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("▲", modifier = Modifier.clickable { 
                                            selectedMinute = (selectedMinute + 5) % 60 
                                        })
                                        Text(String.format("%02d", selectedMinute), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                        Text("▼", modifier = Modifier.clickable { 
                                            selectedMinute = if (selectedMinute < 5) 55 else selectedMinute - 5 
                                        })
                                    }
                                }
                                
                                // AM/PM indicator
                                Box(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .size(60.dp, 60.dp)
                                        .background(Color(0xFF1976D2), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (selectedHour < 12) "AM" else "PM",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
                
                Text("Quick Templates:", fontSize = 14.sp, color = Color.Gray)
                
                templates.forEach { (title, message) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                titleMl = title
                                messageMl = message
                                selectedTemplate = title
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedTemplate == title) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(message, fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Or create custom:", fontSize = 14.sp, color = Color.Gray)
                
                OutlinedTextField(
                    value = messageMl,
                    onValueChange = { 
                        messageMl = it
                        selectedTemplate = ""
                        if (titleMl.isBlank()) titleMl = it.take(20)
                    },
                    label = { Text("Message (Malayalam)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (messageMl.isNotBlank()) {
                        val elderPhone = userPrefs.getLinkedElderPhone()
                        
                        if (elderPhone.isNotBlank()) {
                            if (sendNow) {
                                // Send instant reminder
                                com.oppam.oppamlauncher.sms.SMSSender.sendReminder(
                                    context,
                                    elderPhone,
                                    messageMl
                                )
                            } else {
                                // Schedule alarm
                                val calendar = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, selectedHour)
                                    set(Calendar.MINUTE, selectedMinute)
                                    set(Calendar.SECOND, 0)
                                    
                                    // If time has passed today, schedule for tomorrow
                                    if (timeInMillis < System.currentTimeMillis()) {
                                        add(Calendar.DAY_OF_MONTH, 1)
                                    }
                                }
                                
                                val alarmId = alarmStorage.generateAlarmId()
                                val timeInMillis = calendar.timeInMillis
                                
                                // Send scheduled alarm to elder's phone
                                val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                    context.getSystemService(android.telephony.SmsManager::class.java)
                                } else {
                                    @Suppress("DEPRECATION")
                                    android.telephony.SmsManager.getDefault()
                                }
                                
                                val scheduleMessage = "OPPAM_ALARM:$alarmId|$timeInMillis|$messageMl"
                                
                                try {
                                    smsManager.sendTextMessage(elderPhone, null, scheduleMessage, null, null)
                                    
                                    val timeStr = String.format("%02d:%02d %s", 
                                        if (selectedHour % 12 == 0) 12 else selectedHour % 12,
                                        selectedMinute,
                                        if (selectedHour < 12) "AM" else "PM"
                                    )
                                    
                                    android.widget.Toast.makeText(
                                        context, 
                                        "✅ Alarm sent to elder's phone for $timeStr", 
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Failed to send alarm: ${e.message}",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            
                            // Save locally for history
                            val reminder = Reminder(
                                id = reminderPrefs.generateReminderId(),
                                titleMl = if (titleMl.isBlank()) messageMl.take(20) else titleMl,
                                titleEn = if (titleMl.isBlank()) messageMl.take(20) else titleMl,
                                messageMl = messageMl,
                                messageEn = messageMl,
                                time = System.currentTimeMillis(),
                                fromCaregiver = userPrefs.getUserName()
                            )
                            onAdd(reminder)
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "No elder phone linked!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            ) {
                Text(if (sendNow) "Send Now" else "Set Alarm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
