@echo off
setlocal enabledelayedexpansion

REM Check for administrator privileges and elevate if needed
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo Requesting administrator privileges...
    powershell -Command "Start-Process -FilePath '%~f0' -ArgumentList '%*' -Verb RunAs"
    exit /b
)

REM ==========================================
REM WedKnots Release and Deployment Script
REM ==========================================
REM This script automates the complete release workflow:
REM 1. Updates Maven version to release version
REM 2. Builds the Maven package
REM 3. Unzips distribution to hosting directory
REM 4. Commits changes to GitHub
REM 5. Tags the release in GitHub
REM 6. Updates version to next SNAPSHOT
REM 7. Commits snapshot version to GitHub
REM
REM Usage: release-deploy.bat <RELEASE_VERSION>
REM Example: release-deploy.bat 1.0.2

REM Get the project root directory (parent of scripts)
cd /d "%~dp0.."
set "PROJECT_ROOT=%CD%"
set "MAVEN_PATH=C:\tools\apache-maven-3.9.11\bin\mvn.cmd"
set "HOSTING_PATH=C:\hosting\wed-knots"

REM Validate Maven is available
if not exist "%MAVEN_PATH%" (
  echo ERROR: Maven not found at %MAVEN_PATH%
  exit /b 1
)

REM Check if version parameter is provided
if "%1"=="" (
  echo ERROR: Release version parameter is required
  echo Usage: release-deploy.bat ^<RELEASE_VERSION^>
  echo Example: release-deploy.bat 1.0.2
  exit /b 1
)

set "RELEASE_VERSION=%1"

REM Extract next snapshot version (increment patch version)
for /f "tokens=1,2,3 delims=." %%A in ("%RELEASE_VERSION%") do (
  set "MAJOR=%%A"
  set "MINOR=%%B"
  set "PATCH=%%C"
)
set /a NEXT_PATCH=%PATCH% + 1
set "NEXT_VERSION=%MAJOR%.%MINOR%.%NEXT_PATCH%-SNAPSHOT"

echo ==========================================
echo WedKnots Release and Deployment Workflow
echo ==========================================
echo Release Version: %RELEASE_VERSION%
echo Next Snapshot: %NEXT_VERSION%
echo Project Root: %PROJECT_ROOT%
echo Hosting Path: %HOSTING_PATH%
echo ==========================================

REM Step 1: Update pom.xml with release version using Maven versions plugin
echo.
echo [STEP 1] Updating pom.xml to version %RELEASE_VERSION% using Maven...
cd /d "%PROJECT_ROOT%"
call "%MAVEN_PATH%" versions:set -DnewVersion=%RELEASE_VERSION% -DgenerateBackupPoms=false
if errorlevel 1 (
  echo ERROR: Failed to update version using Maven
  exit /b 1
)
echo Version updated successfully using Maven versions plugin.

REM Step 2: Build Maven package
echo.
echo [STEP 2] Building Maven package...
cd /d "%PROJECT_ROOT%"
call "%MAVEN_PATH%" clean package -DskipTests
if errorlevel 1 (
  echo ERROR: Maven build failed
  exit /b 1
)
echo Maven build completed successfully.

REM Step 3: Unzip distribution to hosting directory
echo.
echo [STEP 3] Unzipping distribution to %HOSTING_PATH%...
if not exist "%HOSTING_PATH%" mkdir "%HOSTING_PATH%"

REM Delete only the specific version folder if it exists
set "VERSION_DIR=%HOSTING_PATH%\wed-knots-%RELEASE_VERSION%"
if exist "%VERSION_DIR%" (
  echo Removing existing version directory: %VERSION_DIR%
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "Remove-Item -Path '%VERSION_DIR%' -Recurse -Force -ErrorAction SilentlyContinue"
)

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "Expand-Archive -Path '%PROJECT_ROOT%\target\wed-knots-%RELEASE_VERSION%.zip' -DestinationPath '%HOSTING_PATH%' -Force"
if errorlevel 1 (
  echo ERROR: Failed to unzip distribution
  exit /b 1
)
echo Distribution unzipped successfully.

REM Step 4: Commit release changes to Git
echo.
echo [STEP 4] Committing release version to GitHub...
cd /d "%PROJECT_ROOT%"
git add -A
git commit -m "Release version %RELEASE_VERSION%"
if errorlevel 1 (
  echo ERROR: Git commit failed
  exit /b 1
)
echo Commit created successfully.

REM Step 5: Tag the release in GitHub
echo.
echo [STEP 5] Creating and pushing tag v%RELEASE_VERSION%...
git tag -a v%RELEASE_VERSION% -m "Release version %RELEASE_VERSION%"
git push origin main
git push origin v%RELEASE_VERSION%
if errorlevel 1 (
  echo ERROR: Git tag or push failed
  exit /b 1
)
echo Tag created and pushed successfully.

REM Step 6: Update pom.xml with next snapshot version using Maven versions plugin
echo.
echo [STEP 6] Updating pom.xml to next snapshot version %NEXT_VERSION% using Maven...
cd /d "%PROJECT_ROOT%"
call "%MAVEN_PATH%" versions:set -DnewVersion=%NEXT_VERSION% -DgenerateBackupPoms=false
if errorlevel 1 (
  echo ERROR: Failed to update pom.xml to snapshot version
  exit /b 1
)
echo Snapshot version updated successfully using Maven versions plugin.

REM Step 7: Commit snapshot version to Git
echo.
echo [STEP 7] Committing snapshot version to GitHub...
cd /d "%PROJECT_ROOT%"
git add pom.xml
git commit -m "Prepare for version %NEXT_VERSION% development"
git push origin main
if errorlevel 1 (
  echo ERROR: Git commit or push failed
  exit /b 1
)
echo Snapshot version committed and pushed successfully.

REM Final Summary
echo.
echo ==========================================
echo Release Workflow Completed Successfully!
echo ==========================================
echo Release Version: %RELEASE_VERSION%
echo Next Development: %NEXT_VERSION%
echo Deployed to: %HOSTING_PATH%\wed-knots-%RELEASE_VERSION%
echo GitHub Tags: https://github.com/anilhemnani/wed-knots/releases/tag/v%RELEASE_VERSION%
echo ==========================================

endlocal
exit /b 0

