# Pack clean source zip for submission (Windows).
# Run from anywhere:
#   powershell -ExecutionPolicy Bypass -File scripts/pack-source.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

python "$Root\scripts\pack_source.py" @args
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
