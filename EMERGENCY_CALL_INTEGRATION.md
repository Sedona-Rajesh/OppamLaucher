# Emergency Call Monitoring Feature - Integration Guide

## Overview
This is a **completely optional, privacy-preserving** emergency call monitoring feature that can be added to the Oppam Launcher without modifying any existing functionality.

## ✅ What's Been Added (Zero Impact on Existing Code)

### New Files Created:
```
app/src/main/java/com/oppam/oppamlauncher/emergency/
├── EmergencyCallMonitorService.kt      # Main monitoring service
├── DistressAnalyzer.kt                 # Real-time distress detection
├── EmergencyAlertManager.kt            # Alert delivery system
├── CallStateListener.kt                # Phone call event listener
├── EmergencyCallPreferences.kt         # User consent & settings
├── EmergencyCallIntegration.kt         # Integration layer
└── ui/
    └── EmergencyCallUI.kt              # UI components (consent, settings, panic button)

app/src/main/res/values/
└── emergency_strings.xml                # String resources for emergency feature
```

### Modified Files:
- **AndroidManifest.xml**: Added optional permissions and service declaration (non-breaking)

## 🔒 Privacy & Security Guarantees

1. **NO AUDIO RECORDING**: Audio is never stored or saved
2. **REAL-TIME ONLY**: Analysis happens in memory during the call
3. **EXPLICIT CONSENT**: User must explicitly enable the feature
4. **CAN BE DISABLED**: User can turn off anytime
5. **AUTHORIZED ACCESS ONLY**: Alerts go only to registered caregiver
6. **NO DATA LEAKAGE**: No personal information is shared beyond the alert

## 📋 How to Integrate (Optional)

### Option 1: Add Consent Screen on First Launch

In `MainActivity.kt`, add this composable call **after** the existing `OppamApp()`:

```kotlin
@Composable
fun OppamApp() {
    var role by remember { mutableStateOf("ELDER") }
    var elderScreen by remember { mutableStateOf("HOME") }
    
    // EXISTING CODE REMAINS UNCHANGED...
    
    // 🆕 OPTIONAL: Add emergency call feature
    val context = LocalContext.current
    val prefs = remember { EmergencyCallPreferences(context) }
    var showEmergencyConsent by remember { 
        mutableStateOf(!prefs.hasConsentGiven() && !prefs.hasDeclinedConsent()) 
    }
    var showEmergencySettings by remember { mutableStateOf(false) }
    
    when {
        showEmergencyConsent -> {
            EmergencyCallConsentScreen(
                onConsent = {
                    EmergencyCallIntegration.initializeIfEnabled(context)
                    showEmergencyConsent = false
                },
                onDecline = {
                    prefs.setDeclineConsent(true)
                    showEmergencyConsent = false
                }
            )
        }
        showEmergencySettings -> {
            EmergencyCallSettingsScreen(
                onBack = { showEmergencySettings = false }
            )
        }
        else -> {
            // EXISTING SCREENS (unchanged)
            when (role) {
                "ELDER" -> { /* existing elder screens */ }
                "CAREGIVER" -> { /* existing caregiver screens */ }
            }
        }
    }
}
```

### Option 2: Add Settings Entry in Caregiver Dashboard

In the `CaregiverDashboard` composable, add this button:

```kotlin
@Composable
fun CaregiverDashboard(
    alerts: List<Alert>,
    onAddReminder: () -> Unit,
    onBack: () -> Unit
) {
    // EXISTING CODE...
    
    // 🆕 OPTIONAL: Emergency call settings button
    ElderButton("🚨 Emergency Call Settings", Color(0xFFFF6F00)) {
        // Navigate to emergency settings
    }
    
    // REST OF EXISTING CODE...
}
```

### Option 3: Standalone Activation

Initialize the service programmatically when needed:

```kotlin
// In onCreate or wherever appropriate
if (EmergencyCallIntegration.needsConsent(this)) {
    // Show consent UI
} else if (EmergencyCallIntegration.initializeIfEnabled(this)) {
    // Service initialized successfully
    Toast.makeText(this, "Emergency monitoring active", Toast.LENGTH_SHORT).show()
}
```

## 🎯 Feature Capabilities

### Automatic Distress Detection:
- ✓ Abnormal silence (configurable threshold)
- ✓ Emergency keywords: "സഹായം", "HELP", "EMERGENCY", "വേദന", "PAIN"
- ✓ Voice anomalies (sudden pitch/energy changes)
- ✓ Sudden call termination after stress indicators

### Manual Emergency Trigger:
- ✓ Panic button overlay during calls
- ✓ One-tap SOS to caregiver

### Caregiver Alerts:
- ✓ SMS alerts to registered family members
- ✓ Detailed distress information
- ✓ Timestamp and severity level

## 🔧 Configuration

All settings are in `EmergencyCallPreferences.kt`:

```kotlin
// Enable/disable feature
prefs.setFeatureEnabled(true/false)

// Check if enabled
prefs.isFeatureEnabled()

// Reset consent
prefs.setConsentGiven(false)
```

## ⚙️ Permissions Required

The following permissions are declared in AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.VIBRATE" />
```

**NOTE**: Permissions are only requested if user enables the feature. App works normally without them.

## 🚀 Testing

1. **Test Consent Flow**:
   ```kotlin
   // Clear preferences to see consent screen again
   EmergencyCallPreferences(context).setConsentGiven(false)
   ```

2. **Test Distress Detection**:
   - Make a call and say "സഹായം" (help in Malayalam)
   - Check caregiver's phone for SMS alert

3. **Test Panic Button**:
   - During a call, trigger the panic overlay
   - Press SOS button
   - Verify alert sent

4. **Test Disable/Enable**:
   - Go to emergency settings
   - Toggle the feature
   - Verify service stops/starts

## 📊 Architecture Flow

```
Phone Call Event
      ↓
CallStateListener (passive observer)
      ↓
EmergencyCallMonitorService
      ↓
DistressAnalyzer (real-time, in-memory)
      ↓
Distress Detected? → Yes → EmergencyAlertManager → SMS to Caregiver
      ↓ No
Continue monitoring
```

## ❌ What This Feature Does NOT Do

- ❌ Does not record audio
- ❌ Does not store conversations
- ❌ Does not modify existing call flow
- ❌ Does not require root access
- ❌ Does not track location
- ❌ Does not share data with third parties
- ❌ Does not run if disabled
- ❌ Does not affect app performance

## 🔄 Removing the Feature

If you want to completely remove this feature:

1. Delete the `emergency/` folder
2. Remove permissions from AndroidManifest.xml
3. Delete `emergency_strings.xml`
4. Remove any integration code from MainActivity (if added)

The app will work exactly as before.

## 📝 Compliance

This feature is designed with HIPAA-like privacy principles:
- ✅ Informed consent required
- ✅ Minimal data collection
- ✅ Purpose limitation (emergency use only)
- ✅ Data minimization (no storage)
- ✅ User control (can disable anytime)
- ✅ Transparency (clear explanation)

## 🆘 Support

For questions or issues with the emergency call feature:
1. Check `EmergencyCallPreferences` for settings
2. Verify permissions are granted
3. Check that caregiver phone numbers are registered
4. Review logs for error messages

---

**Remember**: This is an **OPTIONAL** feature. The core Oppam app works perfectly without it.
