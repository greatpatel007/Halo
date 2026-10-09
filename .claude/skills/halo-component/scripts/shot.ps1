<#
.SYNOPSIS
  Build → install → open a Showroom page by deep link → screenshot. The visual half of the quality gate.

.DESCRIPTION
  -SlowMo / -Grid / -Reduced are passed as deep-link query parameters to the Showroom debug dock, so
  mid-animation captures are reproducible (e.g. 5x slow-mo, then drive a gesture and capture).
  -Theme dark|light|system sets the appearance first. Every component is verified in BOTH themes.
  -NoBuild skips Gradle (reuse the installed APK). -Out picks the PNG path (default shots/<page>.png).

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File .claude/skills/halo-component/scripts/shot.ps1 -Page button
  powershell -ExecutionPolicy Bypass -File .claude/skills/halo-component/scripts/shot.ps1 -Page button -NoBuild -SlowMo 5 -Grid 8 -Out shots/button-grid.png
#>
param(
    [Parameter(Mandatory = $true)][string]$Page,
    [string]$Out = "",
    [switch]$NoBuild,
    [int]$SettleMs = 1500,
    [float]$SlowMo = 1,
    [ValidateSet(0, 4, 8)][int]$Grid = 0,
    [switch]$Reduced,
    [ValidateSet("", "dark", "light", "system")][string]$Theme = "",
    [string]$Avd = "Pixel_10_Pro"
)

# No ErrorActionPreference=Stop: Windows PowerShell 5.1 turns adb/gradle stderr progress into
# terminating errors. Exit codes are checked explicitly instead.
$root = (Resolve-Path "$PSScriptRoot\..\..\..\..").Path
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$adb = "$sdk\platform-tools\adb.exe"
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr" }
if (-not $Out) { $Out = "shots\$Page.png" }
$OutPath = Join-Path $root $Out
New-Item -ItemType Directory -Force (Split-Path $OutPath) | Out-Null

# 1. Device: reuse a running one, otherwise boot the AVD and wait for boot_completed.
$devices = & $adb devices | Select-String -Pattern "\tdevice$"
if (-not $devices) {
    Write-Host "Booting $Avd..."
    Start-Process -FilePath "$sdk\emulator\emulator.exe" -ArgumentList "-avd", $Avd, "-no-snapshot-save", "-no-boot-anim" -WindowStyle Minimized
    & $adb wait-for-device
    $deadline = (Get-Date).AddMinutes(4)
    while ((& $adb shell getprop sys.boot_completed 2>$null) -ne "1") {
        if ((Get-Date) -gt $deadline) { throw "Emulator did not finish booting" }
        Start-Sleep -Seconds 2
    }
}

# 2. Build + install (installing restarts the app process, so every shot starts clean).
Push-Location $root
try {
    if (-not $NoBuild) {
        & .\gradlew.bat :app:assembleDebug --console=plain -q
        if ($LASTEXITCODE -ne 0) { throw "Build failed" }
        & $adb install -r -d "app\build\outputs\apk\debug\app-debug.apk" | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Install failed" }
    }
} finally { Pop-Location }

# 3. Open the page (+ debug dock parameters) and let enter choreography settle.
$query = "?slowmo=$SlowMo&grid=$Grid&rm=$(if ($Reduced) { 1 } else { 0 })"
if ($Theme) { $query += "&theme=$Theme" }
& $adb shell am start -W -a android.intent.action.VIEW -d "'halo://showroom/$Page$query'" com.halo.wallet | Out-Null
Start-Sleep -Milliseconds $SettleMs

# 4. Screenshot (binary-safe: capture on device, then pull).
& $adb shell screencap -p /sdcard/halo_shot.png
& $adb pull /sdcard/halo_shot.png $OutPath | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Screenshot pull failed" }
& $adb shell rm /sdcard/halo_shot.png
Write-Host "Saved $OutPath"
