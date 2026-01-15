@echo off
setlocal enabledelayedexpansion


REM ==========================================
REM WedKnots - Stop Script (Windows)
REM ==========================================

REM Resolve paths
set "BIN_DIR=%~dp0"
for %%I in ("%BIN_DIR:~0,-1%") do set "APP_ROOT=%%~dpI"
set "APP_ROOT=%APP_ROOT:~0,-1%"
for %%I in ("%APP_ROOT%") do set "PARENT_DIR=%%~dpI"
set "PARENT_DIR=%PARENT_DIR:~0,-1%"

set "SERVICE_NAME=WedKnots"
set "PID_FILE=%PARENT_DIR%\%SERVICE_NAME%.pid"

REM Try to stop the Windows service first
echo Attempting to stop %SERVICE_NAME% service...
sc query %SERVICE_NAME% >nul 2>&1
if %errorLevel% equ 0 (
  sc stop %SERVICE_NAME% >nul 2>&1
  if %errorLevel% equ 0 (
    echo Service stopped successfully.
    timeout /t 3 >nul
  ) else (
    echo Service stop command issued, waiting...
    timeout /t 3 >nul
  )
) else (
  echo %SERVICE_NAME% service not found.
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

REM Clean up PID file
if exist "%PID_FILE%" (
  del "%PID_FILE%"
  echo PID file removed: %PID_FILE%
)

echo.
echo Application stopped successfully.
echo.

endlocal

