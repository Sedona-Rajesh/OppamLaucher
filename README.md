# Oppam Launcher

Oppam is a simplified Android launcher for elder care. It delivers caregiver-scheduled alarms via SMS with Malayalam TTS, a clear YES/NO confirmation screen, and consent-based foreground location sharing. It avoids silent tracking and unnecessary status texts.

## Features
- Elder-friendly launcher (`HOME` activity) with large buttons.
- Caregiver → Elder scheduled alarms over SMS (`OPPAM_ALARM:id|time|message`).
- Alarm screen: 5s tone + vibration + Malayalam TTS + YES/NO.
- Follow-up logic: re-ring at 10s; mark `confirmed` / `not_completed` / `no_response`.
- Foreground-only location sharing from elder sends one `OPPAM_LOC` SMS per session.
- Continuous status/ACK/SOS SMS disabled to reduce spam.
- Reset login via broadcast for quick re-setup.

## Permissions
Grant on first launch (elder phone):
- SMS: send/receive
- Notifications
- Exact alarms: Settings → Special app access → Alarms & reminders → Allow
- Location: While using the app
- Battery: Apps → Oppam → Battery → Unrestricted

## Build

From project root:
```powershell
# Build debug APK
.\gradlew.bat assembleDebug
```

## Install & Run (VS Code / PowerShell)

Using Android SDK `adb`:
```powershell
# Replace <DEVICE_ID> with value from `adb devices`
& "C:\Users\<YOU>\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices
.\gradlew.bat :app:installDebug
& "C:\Users\<YOU>\AppData\Local\Android\Sdk\platform-tools\adb.exe" -s <DEVICE_ID> shell am start -n com.oppam.oppamlauncher/.MainActivity
```

If you get `unauthorized`:
```powershell
& "C:\Users\<YOU>\AppData\Local\Android\Sdk\platform-tools\adb.exe" kill-server
& "C:\Users\<YOU>\AppData\Local\Android\Sdk\platform-tools\adb.exe" start-server
& "C:\Users\<YOU>\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices
# Accept the USB debugging prompt on the phone
```

## Make Oppam the Default Home
- Press the phone's Home button → choose Oppam → Always.
- If no chooser appears: Settings → Apps → current launcher → Open by default → Clear defaults → press Home again.

## Login & Linking
- Elder: enter elder name/phone and caregiver name/phone.
- Caregiver: enter caregiver name/phone, optionally link elder phone.
- Malayalam label change: Elder role shown as “വയോധികൻ”.

## Scheduling Alarms (Caregiver)
- Open caregiver UI → Set Reminders → "Schedule Alarm" → choose time.
- Sends: `OPPAM_ALARM:<alarmId>|<timeInMillis>|<messageMl>` to elder.
- Elder phone shows toast “അലാറം ഷെഡ്യൂൾ ചെയ്തു …” and rings at time.

## Location Sharing (Elder)
- Tap "Share Location" → a foreground notification appears.
- First GPS fix sends one `OPPAM_LOC:lat,lng|accuracy|timestamp` SMS to caregiver.
- Caregiver sees last location; map if Google Play services are available.

## Reset to Login (Broadcast)
If numbers were entered wrong:
```powershell
& "C:\Users\<YOU>\AppData\Local\Android\Sdk\platform-tools\adb.exe" -s <DEVICE_ID> shell am broadcast -a com.oppam.oppamlauncher.FORCE_LOGOUT
& "C:\Users\<YOU>\AppData\Local\Android\Sdk\platform-tools\adb.exe" -s <DEVICE_ID> shell am start -n com.oppam.oppamlauncher/.MainActivity
```

## Troubleshooting
- INSTALL_FAILED_UPDATE_INCOMPATIBLE: uninstall previous debug build
```powershell
.\gradlew.bat :app:uninstallDebug
.\gradlew.bat :app:installDebug
```
- INSTALL_FAILED_VERIFICATION_FAILURE: enable install from debug, or try reinstall.
- Alarms not firing: ensure "Alarms & reminders" allowed; verify time is in the future; SMS actually sent (SIM quota).
- `OPPAM_ALARM` not parsed: long SMS may split; receiver now combines multipart PDUs.
- `adb unauthorized`: reconnect and accept the prompt.

## Protocols
- Instant reminder: `OPPAM:<message>`
- Scheduled alarm: `OPPAM_ALARM:<id>|<timeInMillis>|<message>`
- Location update: `OPPAM_LOC:<lat>,<lng>|<accuracy>|<timestamp>`

## Notes
- Foreground-service type `location`; no background tracking.
- Alarm scheduling uses `setAlarmClock` with fallbacks.

---
Made with care for elder safety and simplicity.
