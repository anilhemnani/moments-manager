@echo off
setlocal

REM Check for administrator privileges and elevate if needed
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo Requesting administrator privileges...
    powershell -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

REM ==========================================
REM WedKnots - Stop Script (Windows)
REM ==========================================

REM Try to stop the Windows service first
echo Attempting to stop WedKnots service...
sc query WedKnots >nul 2>&1
if %errorLevel% equ 0 (
  sc stop WedKnots >nul 2>&1
  if %errorLevel% equ 0 (
    echo Service stopped successfully.
    timeout /t 3 >nul
  ) else (
    echo Service stop command issued, waiting...
    timeout /t 3 >nul
  )
) else (
  echo WedKnots service not found.
)

REM Force kill any remaining java.exe processes
echo Checking for running Java processes...
tasklist /fi "IMAGENAME eq java.exe" 2>nul | find /i "java.exe" >nul
if %errorLevel% equ 0 (
  echo Stopping java.exe processes...
  taskkill /F /IM java.exe >nul 2>&1
  timeout /t 2 >nul
  echo Java processes terminated.
) else (
  echo No Java processes found.
)

echo Application stopped.
:done
endlocal

