@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java 21 is required. Install JDK 21 and add java to PATH.
  pause
  exit /b 1
)
if exist "app\pool-desktop.jar" (
  java -Dfile.encoding=UTF-8 -cp "app\pool-desktop.jar;app\lib\*;app\native\win\*" ru.mirea.pool.Main
) else (
  call mvnw.cmd javafx:run
)
if errorlevel 1 pause
