# Oppam Launcher - Windows PowerShell helper commands
# Usage examples:
#   ./scripts/run.ps1 List-Devices
#   ./scripts/run.ps1 Build-Debug
#   ./scripts/run.ps1 Install-Debug
#   ./scripts/run.ps1 Launch-Oppam -DeviceId 2198d9c
#   ./scripts/run.ps1 Force-Login -DeviceId 2198d9c
#   ./scripts/run.ps1 Install-And-Launch -DeviceId 2198d9c

param(
    [Parameter(Position=0, Mandatory=$true)]
    [ValidateSet("List-Devices","Restart-Adb","Build-Debug","Install-Debug","Uninstall-Debug","Launch-Oppam","Force-Login","Install-And-Launch","Set-HomeOppam")]
    [string]$Action,

    [Parameter(Mandatory=$false)]
    [string]$DeviceId
)

function Get-AdbPath {
    $sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $sdk) { return $sdk }
    throw "adb.exe not found. Install Android SDK platform-tools or set PATH."
}

$adb = Get-AdbPath

switch ($Action) {
    "List-Devices" {
        & $adb devices
    }
    "Restart-Adb" {
        & $adb kill-server
        & $adb start-server
        & $adb devices
    }
    "Build-Debug" {
        & .\gradlew.bat assembleDebug
    }
    "Install-Debug" {
        & .\gradlew.bat :app:installDebug
    }
    "Uninstall-Debug" {
        & .\gradlew.bat :app:uninstallDebug
    }
    "Launch-Oppam" {
        if (-not $DeviceId) { throw "-DeviceId is required" }
        & $adb -s $DeviceId shell am start -n com.oppam.oppamlauncher/.MainActivity
    }
    "Force-Login" {
        if (-not $DeviceId) { throw "-DeviceId is required" }
        & $adb -s $DeviceId shell am broadcast -a com.oppam.oppamlauncher.FORCE_LOGOUT
        & $adb -s $DeviceId shell am start -n com.oppam.oppamlauncher/.MainActivity
    }
    "Install-And-Launch" {
        if (-not $DeviceId) { throw "-DeviceId is required" }
        & .\gradlew.bat assembleDebug
        & .\gradlew.bat :app:installDebug
        & $adb -s $DeviceId shell am start -n com.oppam.oppamlauncher/.MainActivity
    }
    "Set-HomeOppam" {
        if (-not $DeviceId) { throw "-DeviceId is required" }
        # Not supported on all devices, but attempt via package manager
        try {
            & $adb -s $DeviceId shell cmd package set-home-activity com.oppam.oppamlauncher/.MainActivity
        } catch {
            Write-Host "Could not set home via shell. Use phone UI: press Home → choose Oppam → Always." -ForegroundColor Yellow
        }
    }
}
