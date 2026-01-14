@echo off
setlocal enabledelayedexpansion

REM Check for administrator privileges and elevate if needed
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo Requesting administrator privileges...
    powershell -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
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
  echo call "%APP_ROOT%\bin\start.bat"
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
powershell -NoProfile -ExecutionPolicy Bypass -Command "try { Stop-Service -Name 'WedKnots' -Force -ErrorAction SilentlyContinue; Start-Sleep -Seconds 2 } catch {} ; try { Remove-Service -Name 'WedKnots' -Force -ErrorAction SilentlyContinue } catch {} ; New-Service -Name 'WedKnots' -BinaryPathName 'cmd.exe /c \"%WRAPPER_BAT%\"' -DisplayName 'WedKnots Service' -StartupType Automatic -ErrorAction SilentlyContinue ; Start-Service -Name 'WedKnots' -ErrorAction SilentlyContinue"
echo Service recreation completed.

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
  exit /b 1
)

echo Starting application with JAR: "%JAR_FILE%"
echo Logging to: %LOG_FILE%

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

endlocal
