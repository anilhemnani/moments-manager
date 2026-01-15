@echo off
setlocal enabledelayedexpansion

REM ==========================================
REM Encrypt a value using JASYPT Maven Plugin
REM Usage: encrypt-password.bat <encryptorPassword> <valueToEncrypt>
REM ==========================================

if "%~1"=="" (
  echo Usage: %~nx0 ^<encryptorPassword^> ^<valueToEncrypt^>
  echo.
  echo Example:
  echo   %~nx0 "my-secret-key" "database-password"
  exit /b 1
)
if "%~2"=="" (
  echo Usage: %~nx0 ^<encryptorPassword^> ^<valueToEncrypt^>
  exit /b 1
)

set "ENC_PASSWORD=%~1"
set "ENC_VALUE=%~2"

REM Resolve project root (scripts folder is under root)
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJ_ROOT=%%~fI"
pushd "%PROJ_ROOT%" >nul

echo.
echo ========================================
echo JASYPT Password Encryption
echo ========================================
echo Encryptor Password: %ENC_PASSWORD%
echo Value to Encrypt: %ENC_VALUE%
echo.

REM Use fully qualified Maven plugin goal
set "MVN_CMD=mvn com.github.ulisesbocchio:jasypt-maven-plugin:4.0.3:encrypt"
set "MVN_CMD=%MVN_CMD% -Djasypt.encryptor.password=%ENC_PASSWORD%"
set "MVN_CMD=%MVN_CMD% -Djasypt.plugin.value=%ENC_VALUE%"

echo Running encryption...
echo.
%MVN_CMD%
set "RC=%ERRORLEVEL%"

echo.
if %RC% equ 0 (
  echo ========================================
  echo Encryption successful!
  echo ========================================
  echo.
  echo Use this encrypted value with ENC() wrapper:
  echo   ENC(encrypted_value_from_above)
) else (
  echo ========================================
  echo ERROR: Encryption failed with exit code %RC%
  echo ========================================
)

popd >nul
exit /b %RC%

