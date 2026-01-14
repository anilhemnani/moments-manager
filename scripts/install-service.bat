@echo off
setlocal enabledelayedexpansion

REM ==========================================
REM WedKnots - Install/Reinstall Windows Service
REM ==========================================
REM Creates Windows service that automatically runs the latest
REM version of WedKnots installed in C:\hosting\wedknots
REM
REM Usage: install-service.bat
REM        install-service.bat /uninstall  (remove service only)

REM Check for administrator privileges
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo.
    echo ==========================================
    echo Administrator Privileges Required
    echo ==========================================
    echo This script requires administrator privileges.
    echo Please run as administrator.
    echo.
    pause
    exit /b 1
)

REM Configuration
set "SERVICE_NAME=WedKnots"
set "DISPLAY_NAME=WedKnots Application Service"
set "HOSTING_ROOT=C:\hosting\wedknots"
set "INSTALL_SCRIPT=%~dp0install-service.ps1"

REM Check hosting folder exists
if not exist "%HOSTING_ROOT%" (
    echo ERROR: Hosting folder not found: %HOSTING_ROOT%
    pause
    exit /b 1
)

REM Handle uninstall parameter
if /i "%1"=="/uninstall" (
    goto uninstallService
)

REM ==========================================
REM Main Installation Flow
REM ==========================================

echo.
echo ==========================================
echo WedKnots Windows Service Installation
echo ==========================================
echo Service Name: %SERVICE_NAME%
echo Display Name: %DISPLAY_NAME%
echo Hosting Root: %HOSTING_ROOT%
echo.

REM Create the PowerShell installation script
call :createPowerShellScript

REM Run PowerShell script to install service
echo Installing service...
powershell -NoProfile -ExecutionPolicy Bypass -File "%INSTALL_SCRIPT%"
if %errorLevel% equ 0 (
    echo.
    echo ==========================================
    echo Service Installed Successfully!
    echo ==========================================
    echo Service Name: %SERVICE_NAME%
    echo Status: Should be running now
    echo.
    echo To check status:
    echo   sc query %SERVICE_NAME%
    echo.
    echo To view service properties:
    echo   services.msc
    echo.
) else (
    echo.
    echo ERROR: Service installation failed!
    echo Check the error messages above.
    echo.
    pause
    exit /b 1
)

goto end

REM ==========================================
REM Uninstall Service
REM ==========================================
:uninstallService
echo.
echo Uninstalling %SERVICE_NAME% service...
echo.

REM Stop the service
sc stop %SERVICE_NAME% >nul 2>&1

REM Wait for service to stop
timeout /t 2 >nul

REM Delete the service
sc delete %SERVICE_NAME% >nul 2>&1
if %errorLevel% equ 0 (
    echo Service removed successfully.
) else (
    echo Warning: Service may not have been fully removed.
    echo You may need to restart your computer.
)

goto end

REM ==========================================
REM Create PowerShell Installation Script
REM ==========================================
:createPowerShellScript

set "PS_SCRIPT=%INSTALL_SCRIPT%"

(
    echo # WedKnots Windows Service Installation Script
    echo # This script installs/reinstalls the WedKnots service
    echo # It finds the latest version in the hosting directory
    echo.
    echo $ServiceName = "%SERVICE_NAME%"
    echo $DisplayName = "%DISPLAY_NAME%"
    echo $HostingRoot = "%HOSTING_ROOT%"
    echo.
    echo # Find latest version folder
    echo $VersionFolder = Get-ChildItem -Path $HostingRoot -Directory -Filter "wed-knots-*" ^| Sort-Object Name -Descending ^| Select-Object -First 1
    echo.
    echo if ($null -eq $VersionFolder) {
    echo     Write-Error "No wed-knots version folders found in $HostingRoot"
    echo     exit 1
    echo }
    echo.
    echo $AppPath = $VersionFolder.FullName
    echo $BinPath = Join-Path $AppPath "bin\start.bat"
    echo $ConfigPath = Join-Path $AppPath "config"
    echo.
    echo Write-Host "Found version: $($VersionFolder.Name)" -ForegroundColor Green
    echo Write-Host "Application path: $AppPath" -ForegroundColor Cyan
    echo Write-Host "Start script: $BinPath" -ForegroundColor Cyan
    echo.
    echo # Verify start.bat exists
    echo if (!(Test-Path $BinPath)) {
    echo     Write-Error "Start script not found: $BinPath"
    echo     exit 1
    echo }
    echo.
    echo # Stop existing service if running
    echo Write-Host "Stopping existing service..." -ForegroundColor Yellow
    echo try {
    echo     $service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    echo     if ($service) {
    echo         Stop-Service -Name $ServiceName -Force -ErrorAction SilentlyContinue
    echo         Start-Sleep -Seconds 2
    echo     }
    echo } catch {
    echo     Write-Host "Service was not running or could not be stopped."
    echo }
    echo.
    echo # Remove existing service
    echo Write-Host "Removing old service..." -ForegroundColor Yellow
    echo try {
    echo     $service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    echo     if ($service) {
    echo         Remove-Service -Name $ServiceName -Force -ErrorAction SilentlyContinue
    echo         Start-Sleep -Seconds 1
    echo         Write-Host "Old service removed." -ForegroundColor Green
    echo     }
    echo } catch {
    echo     Write-Host "Could not remove old service, continuing..."
    echo }
    echo.
    echo # Create new service
    echo Write-Host "Creating new service..." -ForegroundColor Yellow
    echo $ServicePath = "`"cmd.exe /c `""`"$BinPath`""`" service`""
    echo.
    echo try {
    echo     New-Service -Name $ServiceName ^`
    echo                 -BinaryPathName $ServicePath ^`
    echo                 -DisplayName $DisplayName ^`
    echo                 -StartupType Automatic ^`
    echo                 -Description "WedKnots Spring Boot Application Service" ^`
    echo                 -ErrorAction Stop ^| Out-Null
    echo     Write-Host "Service created successfully." -ForegroundColor Green
    echo } catch {
    echo     Write-Error "Failed to create service: $_"
    echo     exit 1
    echo }
    echo.
    echo # Start the service
    echo Write-Host "Starting service..." -ForegroundColor Yellow
    echo try {
    echo     Start-Service -Name $ServiceName -ErrorAction Stop
    echo     Start-Sleep -Seconds 3
    echo     Write-Host "Service started successfully." -ForegroundColor Green
    echo } catch {
    echo     Write-Error "Failed to start service: $_"
    echo     exit 1
    echo }
    echo.
    echo # Verify service is running
    echo Write-Host "Verifying service status..." -ForegroundColor Yellow
    echo $svc = Get-Service -Name $ServiceName
    echo if ($svc.Status -eq "Running") {
    echo     Write-Host "Service is running!" -ForegroundColor Green
    echo } else {
    echo     Write-Warning "Service is not running. Status: $($svc.Status)"
    echo     exit 1
    echo }
    echo.
    echo Write-Host ""
    echo Write-Host "Service Information:" -ForegroundColor Cyan
    echo Write-Host "  Name: $($svc.Name)"
    echo Write-Host "  Display Name: $($svc.DisplayName)"
    echo Write-Host "  Status: $($svc.Status)"
    echo Write-Host "  Startup Type: $($svc.StartupType)"
    echo Write-Host ""
    echo exit 0
) > "%PS_SCRIPT%"

exit /b 0

REM ==========================================
REM End
REM ==========================================
:end
endlocal
exit /b 0

