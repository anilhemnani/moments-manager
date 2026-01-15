@echo off
setlocal enabledelayedexpansion

REM Resolve application root directory
REM BIN_DIR = C:\hosting\wed-knots\wed-knots-1.0.9\bin\
set "BIN_DIR=%~dp0"

REM APP_ROOT = C:\hosting\wed-knots\wed-knots-1.0.9
for %%I in ("%BIN_DIR:~0,-1%") do set "APP_ROOT=%%~dpI"
set "APP_ROOT=%APP_ROOT:~0,-1%"

REM PARENT_DIR = C:\hosting\wed-knots
for %%I in ("%APP_ROOT%") do set "PARENT_DIR=%%~dpI"
set "PARENT_DIR=%PARENT_DIR:~0,-1%"

REM HOSTING_DIR = C:\hosting
for %%I in ("%PARENT_DIR%") do set "HOSTING_DIR=%%~dpI"
set "HOSTING_DIR=%HOSTING_DIR:~0,-1%"

cd /d "%APP_ROOT%"

REM Extract version from APP_ROOT folder name (wed-knots-1.0.9 -> 1.0.9)
for %%I in ("%APP_ROOT%") do set "VERSION_FOLDER=%%~nxI"
set "VERSION=%VERSION_FOLDER:wed-knots-=%"
set "SERVICE_NAME=WedKnots"


REM Stop existing Java processes
echo Stopping existing application...
tasklist /fi "IMAGENAME eq java.exe" 2>nul | find /i "java.exe" >nul
if %errorLevel% equ 0 (
  taskkill /F /IM java.exe >nul 2>&1
  timeout /t 2 >nul
  echo Stopped existing java.exe
)

REM Remove old PID file if exists
set "PID_FILE=%PARENT_DIR%\%SERVICE_NAME%.pid"
if exist "%PID_FILE%" del "%PID_FILE%"

REM Ensure logs directory exists
if not exist "%APP_ROOT%\logs" mkdir "%APP_ROOT%\logs"

REM Find JAR file
set "JAR_FILE="
for /f "delims=" %%J in ('dir /b "%APP_ROOT%\app\wed-knots-*.jar" 2^>nul') do set "JAR_FILE=%APP_ROOT%\app\%%J"
if not defined JAR_FILE (
  echo ERROR: Spring Boot JAR not found in app\ folder
  pause
  exit /b 1
)

echo.
echo Starting WedKnots Application
echo ================================
echo Service: %SERVICE_NAME%
echo Version: %VERSION%
echo JAR: %JAR_FILE%
echo Log: %LOG_FILE%
echo.
echo Current directory: %cd%
echo PARENT_DIR: %PARENT_DIR%
echo HOSTING_DIR: %HOSTING_DIR%
echo.

REM Load environment variables from config files
call %BIN_DIR%load-env.bat "%PARENT_DIR%\config.env"
call %BIN_DIR%load-env.bat "%APP_ROOT%\config\config.env"


REM Build Java command line arguments with all loaded variables
set "JAVA_OPTS=-Xms512m -Xmx1024m"


echo.
echo Java Options Built:
echo !JAVA_OPTS!
echo.

REM Start application based on mode
if "%1"=="service" (
  echo Mode: Background Service
  start "WedKnots" /B java !JAVA_OPTS! -jar "%JAR_FILE%"

  REM Wait for process to start and get PID
  timeout /t 2 >nul
  call :writePidFile

  timeout /t 1 >nul
  echo Monitoring Java process...
  :monitor_loop
  tasklist /fi "IMAGENAME eq java.exe" 2>nul | find /i "java.exe" >nul
  if %errorLevel% equ 0 (
    timeout /t 10 >nul
    goto monitor_loop
  )
  echo Java process terminated
  REM Clean up PID file
  if exist "%PID_FILE%" del "%PID_FILE%"
) else (
  echo Mode: Foreground Console
  java !JAVA_OPTS! -jar "%JAR_FILE%"
)

endlocal
exit /b 0

REM ==========================================
REM Function to write PID file
REM ==========================================
:writePidFile
REM Get the PID of the most recently started java process
for /f "tokens=2" %%A in ('tasklist /fi "IMAGENAME eq java.exe" /fo csv 2^>nul ^| find /i "java.exe"') do set "JAVA_PID=%%A"

if defined JAVA_PID (
  (
    echo ServiceName=%SERVICE_NAME%
    echo Version=%VERSION%
    echo PID=!JAVA_PID!
    echo StartTime=%date% %time%
  ) > "%PID_FILE%"
  echo Created PID file: %PID_FILE%
  echo Service: %SERVICE_NAME%, Version: %VERSION%, PID: !JAVA_PID!
) else (
  echo Warning: Could not determine Java process PID
)
exit /b 0

