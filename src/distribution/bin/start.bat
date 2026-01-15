@echo off
setlocal enabledelayedexpansion

set "BIN_DIR=%~dp0"

for %%I in ("%BIN_DIR:~0,-1%") do set "APP_ROOT=%%~dpI"
set "APP_ROOT=%APP_ROOT:~0,-1%"

for %%I in ("%APP_ROOT%") do set "PARENT_DIR=%%~dpI"
set "PARENT_DIR=%PARENT_DIR:~0,-1%"

for %%I in ("%PARENT_DIR%") do set "HOSTING_DIR=%%~dpI"
set "HOSTING_DIR=%HOSTING_DIR:~0,-1%"

cd /d "%APP_ROOT%"

for %%I in ("%APP_ROOT%") do set "VERSION_FOLDER=%%~nxI"
set "VERSION=%VERSION_FOLDER:wed-knots-=%"
set "SERVICE_NAME=WedKnots"


echo Stopping existing application...
tasklist /fi "IMAGENAME eq java.exe" 2>nul | find /i "java.exe" >nul
if %errorLevel% equ 0 (
  taskkill /F /IM java.exe >nul 2>&1
  timeout /t 2 >nul
  echo Stopped existing java.exe
)

set "PID_FILE=%PARENT_DIR%\%SERVICE_NAME%.pid"
if exist "%PID_FILE%" del "%PID_FILE%"

if not exist "%APP_ROOT%\logs" mkdir "%APP_ROOT%\logs"

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

call %BIN_DIR%load-env.bat "%PARENT_DIR%\config.env"
call %BIN_DIR%load-env.bat "%APP_ROOT%\config\config.env"



set "JAVA_OPTS=-Xms512m -Xmx1024m"


echo.
echo Java Options Built:
echo !JAVA_OPTS!
echo.

echo Mode: Background Service
java !JAVA_OPTS! -jar "%JAR_FILE%" 2>&1

