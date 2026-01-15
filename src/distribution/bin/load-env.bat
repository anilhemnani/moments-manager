@echo off

REM ==========================================
REM Load Environment Variables from .env File
REM ==========================================
REM Usage: load_env.bat <path-to-.env-file>
REM Example: load_env.bat C:\hosting\config.env
REM ==========================================

if "%~1"=="" (
  echo Usage: %~nx0 ^<path-to-.env-file^>
  echo.
  echo Example:
  echo   %~nx0 C:\hosting\config.env
  echo   %~nx0 ..\config.env
  exit /b 1
)

set "ENV_FILE=%~1"

if not exist "%ENV_FILE%" (
  echo WARNING: Environment file not found: %ENV_FILE%
  echo Skipping load. No changes made to environment.
  exit /b 0
)
echo Loading environment variables from: %ENV_FILE%

REM Read file and set variables
for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
    set "LINE=%%A"
    if defined LINE (
        set "FIRST_CHAR=!LINE:~0,1!"
        if not "!FIRST_CHAR!"=="#" (
            set "%%A=%%B"
            echo   [!LINE!]
        )
    )
)

echo Environment variables loaded successfully!

