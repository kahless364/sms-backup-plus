# start-dashboard.ps1 — Launch the SMS Backup+ SDLC artifact dashboard
# Usage:  pwsh -File .\start-dashboard.ps1          (or right-click > Run with PowerShell)
#         pwsh -File .\start-dashboard.ps1 -Port 3818

param(
    [int]$Port = 3818
)

$ErrorActionPreference = 'Stop'

# Project root = the directory this script lives in
$Project = $PSScriptRoot

# Find the latest installed amp plugin version (survives plugin updates)
$pluginBase = Join-Path $env:USERPROFILE '.claude\plugins\cache\ai-value-lab-dev\amp'
if (-not (Test-Path $pluginBase)) {
    Write-Error "amp plugin cache not found at $pluginBase. Is the plugin installed?"
    exit 1
}

# Pick the highest semver-looking version directory that has dashboard/bootstrap.mjs
$pluginRoot = Get-ChildItem $pluginBase -Directory |
    Where-Object { Test-Path (Join-Path $_.FullName 'dashboard\bootstrap.mjs') } |
    Sort-Object { [version]($_.Name -replace '[^0-9.].*$','') } -ErrorAction SilentlyContinue |
    Select-Object -Last 1

if (-not $pluginRoot) {
    Write-Error "No amp plugin version with dashboard/bootstrap.mjs found under $pluginBase"
    exit 1
}

$bootstrap = Join-Path $pluginRoot.FullName 'dashboard\bootstrap.mjs'
$overviewUrl = "http://localhost:$Port/api/overview"

# Already running?
try {
    Invoke-WebRequest -Uri $overviewUrl -UseBasicParsing -TimeoutSec 3 | Out-Null
    Write-Host "Dashboard already running at http://localhost:$Port" -ForegroundColor Green
    exit 0
} catch {
    # not running — continue to start it
}

Write-Host "Starting dashboard (plugin $($pluginRoot.Name)) on port $Port ..."
Start-Process -FilePath 'node' `
    -ArgumentList @($bootstrap, '--port', "$Port", '--project', $Project) `
    -WindowStyle Hidden

# Wait up to ~10s for it to come up
for ($i = 0; $i -lt 10; $i++) {
    Start-Sleep -Seconds 1
    try {
        Invoke-WebRequest -Uri $overviewUrl -UseBasicParsing -TimeoutSec 2 | Out-Null
        Write-Host "Dashboard is running at http://localhost:$Port" -ForegroundColor Green
        exit 0
    } catch { }
}

Write-Warning "Dashboard did not respond within 10s. It may still be starting — check http://localhost:$Port"
exit 1
