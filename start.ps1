$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
docker compose up -d --wait db
if ($LASTEXITCODE -ne 0) { throw 'Start Docker Desktop and try again.' }
& .\run.bat
