<#
.SYNOPSIS
  Finds on-screen elements by accessibility label and prints their bounds + centre in device pixels —
  use the centre for `adb shell input tap|swipe` instead of guessing coordinates from a screenshot.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File .claude/skills/halo-component/scripts/find.ps1 -Desc "Hold to send"
  # → Hold to send 21,000 sats   [108,1185][1172,1377]   centre 640 1281
#>
param([Parameter(Mandatory = $true)][string]$Desc)

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$adb = "$sdk\platform-tools\adb.exe"
$tmp = Join-Path $env:TEMP "halo_ui.xml"
& $adb shell uiautomator dump /sdcard/halo_ui.xml | Out-Null
& $adb pull /sdcard/halo_ui.xml $tmp | Out-Null
[xml]$ui = Get-Content $tmp -Raw
$nodes = $ui.SelectNodes("//node") | Where-Object { $_.'content-desc' -like "*$Desc*" -or $_.text -like "*$Desc*" }
if (-not $nodes) { Write-Host "No element matching '$Desc'"; exit 1 }
foreach ($n in $nodes) {
    if ($n.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
        $cx = [int](([int]$Matches[1] + [int]$Matches[3]) / 2)
        $cy = [int](([int]$Matches[2] + [int]$Matches[4]) / 2)
        $label = if ($n.'content-desc') { $n.'content-desc' } else { $n.text }
        Write-Host ("{0,-40} {1,-26} centre {2} {3}" -f $label, $n.bounds, $cx, $cy)
    }
}
