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

REM Try graceful stop by killing java.exe
for /f "tokens=2 delims=," %%P in ('tasklist /fi "IMAGENAME eq java.exe" /fo csv 2^>nul') do (
  echo Stopping existing java.exe...
  taskkill /F /IM java.exe >nul 2>&1
  timeout /t 2 >nul
  goto :done
)

echo No running application found.
:done
endlocal

