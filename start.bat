@echo off
setlocal
cd /d "%~dp0"
docker compose up -d --wait db
if errorlevel 1 (
  echo Start Docker Desktop and try again.
  pause
  exit /b 1
)
call run.bat
