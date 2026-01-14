@echo off
setlocal enabledelayedexpansion

REM Check for administrator privileges and elevate if needed
REM Only check if not already elevated (ELEVATED env var set by elevated instance)
if not defined ELEVATED (
    net session >nul 2>&1
    if %errorLevel% neq 0 (
        echo.
        echo ==========================================
        echo Administrative Privileges Required
        echo ==========================================
        echo This script requires administrator privileges.
        echo Please approve the elevation request...
        echo.

        REM Set flag to indicate we're attempting elevation
        set "ELEVATED=1"
        REM Re-run script as administrator
        powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process cmd.exe -ArgumentList '/c \"cd /d \"%CD%\" && set ELEVATED=1 && call \"%~f0\" %*' -Verb RunAs" 2>nul

        if !errorLevel! neq 0 (
            echo.
            echo ERROR: Failed to elevate privileges!
            echo Please run this script as administrator.
            echo.
            echo To run as administrator:
            echo   1. Right-click on start.bat
            echo   2. Select "Run as administrator"
            echo.
            pause
        )
        exit /b !errorLevel!
    )
)

REM ==========================================
REM WedKnots - Start Script (Windows)
REM ==========================================
REM - Loads parent config.env (one level above install dir)
REM - Then loads config\config.env to override
REM - Stops existing app and starts new version
REM - Creates wrapper bat in parent folder
REM - Deletes and recreates WedKnots service

REM Resolve install root (bin is under install root)
set "BIN_DIR=%~dp0"
for %%I in ("%BIN_DIR%..") do set "APP_ROOT=%%~fI"
cd /d "%APP_ROOT%"

REM Resolve parent directory
for %%I in ("%APP_ROOT%..") do set "PARENT_DIR=%%~fI"

REM --- Create wrapper bat file in parent folder ---
set "WRAPPER_BAT=%PARENT_DIR%\WedKnots.bat"
(
  echo @echo off
  echo setlocal enabledelayedexpansion
  echo.
  echo REM Auto-generated wrapper for WedKnots service
  echo REM This file invokes the main start.bat script
  echo.
  echo REM Set working directory
  echo cd /d "%APP_ROOT%"
  echo.
  echo REM Call the actual start script with service parameter
  echo call "%APP_ROOT%\bin\start.bat" service
  echo endlocal
) > "%WRAPPER_BAT%"
echo Created/Updated wrapper: "%WRAPPER_BAT%"

REM --- Load parent config.env if present ---
set "PARENT_ENV=%APP_ROOT%\..\config.env"
if exist "%PARENT_ENV%" (
  echo Loading parent config: "%PARENT_ENV%"
  for /f "usebackq delims=" %%A in ("%PARENT_ENV%") do (
    if not "%%A"=="" if /i not "%%A:~0,1%%"=="#" set "%%A"
  )
) else (
  echo No parent config.env found at ..\config.env
)

REM --- Load child config/config.env to override ---
set "CHILD_ENV=%APP_ROOT%\config\config.env"
if exist "%CHILD_ENV%" (
  echo Loading child config: "%CHILD_ENV%"
  for /f "usebackq delims=" %%A in ("%CHILD_ENV%") do (
    if not "%%A"=="" if /i not "%%A:~0,1%%"=="#" set "%%A"
  )
) else (
  echo No child config/config.env found
)

REM Defaults
if not defined SPRING_PROFILES_ACTIVE set "SPRING_PROFILES_ACTIVE=prod"
if not defined PORT set "PORT=8080"
if not defined LOG_FILE set "LOG_FILE=logs/wedknots.log"

REM --- Delete and recreate WedKnots service ---
echo Deleting and recreating WedKnots service...
REM First stop and remove old service
sc stop WedKnots >nul 2>&1
timeout /t 2 >nul
sc delete WedKnots >nul 2>&1
timeout /t 1 >nul

REM Create service using sc with proper parameters
sc create WedKnots binPath= "cmd.exe /c \"\"%WRAPPER_BAT%\"\"" start= auto DisplayName= "WedKnots Service" >nul 2>&1
if %errorLevel% equ 0 (
  echo Service created successfully.
  sc description WedKnots "WedKnots Spring Boot Application Service"
  sc start WedKnots >nul 2>&1
  if %errorLevel% equ 0 (
    echo Service started successfully.
  ) else (
    echo Warning: Service created but failed to start. Starting manually...
    goto startManually
  )
) else (
  echo Warning: Service creation failed. Starting application manually...
  goto startManually
)
goto end

:startManually
REM --- Stop existing Java process for this app ---
echo Checking for existing application...
for /f "tokens=2 delims=," %%P in ('tasklist /fi "IMAGENAME eq java.exe" /fo csv 2^>nul') do (
  REM Blind kill any running java (customize with pid file if needed)
  echo Stopping existing java.exe...
  taskkill /F /IM java.exe >nul 2>&1
  timeout /t 2 >nul
  goto afterStop
)
:afterStop

REM Ensure logs directory exists
if not exist "%APP_ROOT%\logs" mkdir "%APP_ROOT%\logs"

REM Locate app jar in app folder
set "JAR_FILE="
for /f "delims=" %%J in ('dir /b "%APP_ROOT%\app\wed-knots-*.jar" 2^>nul') do set "JAR_FILE=%APP_ROOT%\app\%%J"
if not defined JAR_FILE (
  echo ERROR: Spring Boot jar not found in app\ folder.
  pause
  exit /b 1
)

echo Starting application with JAR: "%JAR_FILE%"
echo Logging to: %LOG_FILE%

REM Check if running as service or app (default to app if no parameter)
if "%1"=="service" (
  REM Running as service - start in background and monitor
  echo Starting in background mode (service)...
  start "WedKnots" /B java -Xms512m -Xmx1024m ^
    -Dspring.profiles.active=%SPRING_PROFILES_ACTIVE% ^
    -Dspring.datasource.url="%DATABASE_URL%" ^
    -Dspring.datasource.username="%DATABASE_USERNAME%" ^
    -Dspring.datasource.password="%DATABASE_PASSWORD%" ^
    -Dspring.datasource.driver-class-name="org.postgresql.Driver" ^
    -Dserver.port=%PORT% ^
    -Dlogging.file.name="%LOG_FILE%" ^
    -Djasypt.encryptor.password="%JASYPT_ENCRYPTOR_PASSWORD%" ^
    -Dwhatsapp.webhook.verify-token="%WHATSAPP_VERIFY_TOKEN%" ^
    -Dwhatsapp.webhook.app-secret="%WHATSAPP_APP_SECRET%" ^
    -jar "%JAR_FILE%"

  REM Wait for process to start
  timeout /t 3 >nul

  REM Monitor the Java process - keep script alive
  echo Monitoring Java process...
  :monitor_loop
  tasklist /fi "IMAGENAME eq java.exe" 2>nul | find /i "java.exe" >nul
  if %errorLevel% equ 0 (
    timeout /t 10 >nul
    goto monitor_loop
  )

  echo Java process has terminated. Exiting monitor.
) else (
  REM Default behavior and explicit app mode - run in foreground
  echo Starting in foreground mode (console - app mode)...
  java -Xms512m -Xmx1024m ^
    -Dspring.profiles.active=%SPRING_PROFILES_ACTIVE% ^
    -Dspring.datasource.url="%DATABASE_URL%" ^
    -Dspring.datasource.username="%DATABASE_USERNAME%" ^
    -Dspring.datasource.password="%DATABASE_PASSWORD%" ^
    -Dspring.datasource.driver-class-name="org.postgresql.Driver" ^
    -Dserver.port=%PORT% ^
    -Dlogging.file.name="%LOG_FILE%" ^
    -Djasypt.encryptor.password="%JASYPT_ENCRYPTOR_PASSWORD%" ^
    -Dwhatsapp.webhook.verify-token="%WHATSAPP_VERIFY_TOKEN%" ^
    -Dwhatsapp.webhook.app-secret="%WHATSAPP_APP_SECRET%" ^
    -jar "%JAR_FILE%"
)

:end
endlocal
