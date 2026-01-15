@echo off
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo Requesting administrator privileges...
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

set "BIN_DIR=%~dp0"
nssm.exe install "WedKnots-Springboot" "%APP_ROOT%\bin\start.bat"
nssm.exe set WedKnots-Springboot AppDirectory "%APP_ROOT%"
nssm.exe set WedKnots-Springboot Start SERVICE_AUTO_START
nssm.exe set WedKnots-Springboot ObjectName LocalSystem
pause