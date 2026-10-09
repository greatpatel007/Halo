<#
.SYNOPSIS
  Pixel review: crops a region of a screenshot and enlarges it with nearest-neighbour scaling, so every single
  device pixel is visible (no smoothing that would hide a flaw). Read the output PNG and inspect edges, corners,
  gradients (banding), seams and shadows.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File .claude/skills/halo-component/scripts/zoom.ps1 -In shots/toggle-dark.png -X 400 -Y 1200 -W 300 -H 160 -Scale 4
#>
param(
    [Parameter(Mandatory = $true)][string]$In,
    [Parameter(Mandatory = $true)][int]$X,
    [Parameter(Mandatory = $true)][int]$Y,
    [int]$W = 240,
    [int]$H = 140,
    [int]$Scale = 4,
    [string]$Out = ""
)
$ffmpeg = (Get-Command ffmpeg -ErrorAction SilentlyContinue).Source
if (-not $ffmpeg) {
    $ffmpeg = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter ffmpeg.exe -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $ffmpeg) { throw "ffmpeg not found (winget install Gyan.FFmpeg)" }
if (-not $Out) { $Out = [IO.Path]::ChangeExtension($In, $null).TrimEnd('.') + "-zoom-$X-$Y.png" }
& $ffmpeg -v error -y -i $In -vf "crop=$($W):$($H):$($X):$($Y),scale=iw*$($Scale):ih*$($Scale):flags=neighbor" $Out
if ($LASTEXITCODE -ne 0) { throw "zoom failed" }
Write-Host "Saved $Out"
