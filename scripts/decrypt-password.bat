@echo off
setlocal enabledelayedexpansion

REM ==========================================
REM Decrypt a value using JASYPT Maven Plugin
REM Usage: decrypt-password.bat <encryptorPassword> <encryptedValue>
REM ==========================================

if "%~1"=="" (
  echo Usage: %~nx0 ^<encryptorPassword^> ^<encryptedValue^>
  echo.
  echo Example:
  echo   %~nx0 "my-secret-key" "pKqL9mN2vX5sT7qW3bR8zP1"
  exit /b 1
)
if "%~2"=="" (
  echo Usage: %~nx0 ^<encryptorPassword^> ^<encryptedValue^>
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
echo JASYPT Password Decryption
echo ========================================
echo Encryptor Password: %ENC_PASSWORD%
echo Encrypted Value: %ENC_VALUE%
echo.

REM Use fully qualified Maven plugin goal
set "MVN_CMD=mvn com.github.ulisesbocchio:jasypt-maven-plugin:4.0.3:decrypt"
set "MVN_CMD=%MVN_CMD% -Djasypt.encryptor.password=%ENC_PASSWORD%"
set "MVN_CMD=%MVN_CMD% -Djasypt.plugin.value=%ENC_VALUE%"

echo Running decryption...
echo.
%MVN_CMD%
set "RC=%ERRORLEVEL%"

echo.
if %RC% equ 0 (
  echo ========================================
  echo Decryption successful!
  echo ========================================
  echo.
  echo The decrypted value is shown in output above
) else (
  echo ========================================
  echo ERROR: Decryption failed with exit code %RC%
  echo ========================================
)

popd >nul
exit /b %RC%

