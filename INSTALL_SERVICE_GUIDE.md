# install-service.bat - Windows Service Installation Guide

## Overview
`install-service.bat` is an automated script that installs/reinstalls the WedKnots Windows service. It can use a specific version of WedKnots from `C:\hosting\wedknots`, or automatically detect and use the latest version if no version is specified.

## Features

✅ **Auto-detects Latest Version** - Finds and uses the newest version folder  
✅ **Admin Privilege Check** - Verifies administrator rights before running  
✅ **Clean Installation** - Removes old service before creating new one  
✅ **Service Validation** - Verifies service is running after installation  
✅ **PowerShell Script Generation** - Creates PowerShell script on-the-fly  
✅ **Uninstall Support** - Can remove service cleanly  
✅ **Colored Output** - Clear, readable console messages  

## Usage

### Install Service with Latest Version (Auto-detect)
```bat
cd C:\dev\projects\wed-knots\scripts
install-service.bat
```

### Install Service with Specific Version
```bat
install-service.bat 1.0.3
install-service.bat 1.0.2
```

### Uninstall Service
```bat
install-service.bat /uninstall
```

## What It Does

1. **Checks Admin Rights** - Ensures script runs as administrator
2. **Verifies Hosting Directory** - Confirms `C:\hosting\wedknots` exists
3. **Finds Version** - Uses specified version, or auto-detects latest if none provided
4. **Validates Version Exists** - Errors if specified version not found, shows available versions
5. **Generates PowerShell Script** - Creates `install-service.ps1` dynamically
6. **Stops Existing Service** - Gracefully stops running WedKnots service
7. **Removes Old Service** - Deletes previous service definition
8. **Creates New Service** - Installs fresh WedKnots service
9. **Starts Service** - Automatically starts the new service
10. **Validates Installation** - Confirms service is running and reports version being used

## Directory Structure

```
C:\
├── hosting\
│   └── wedknots\
│       ├── wed-knots-1.0.2\
│       │   ├── bin\
│       │   │   └── start.bat  ← Service calls this with "service" parameter
│       │   ├── config\
│       │   │   └── config.env
│       │   └── app\
│       │       └── wed-knots-1.0.2.jar
│       └── wed-knots-1.0.3\  ← Latest version, will be used
│           └── ...
└── dev\
    └── projects\
        └── wed-knots\
            └── scripts\
                └── install-service.bat  ← Run this
```

## Generated PowerShell Script

The script automatically generates `install-service.ps1` in the same directory as `install-service.bat`. This PowerShell script:

- Accepts a version parameter (set by the batch script)
- If version specified: looks for exact version folder `wed-knots-X.X.X`
- If no version: searches for latest `wed-knots-*` version folder
- Lists available versions if specified version not found
- Verifies `start.bat` exists in the chosen version
- Creates Windows service entry pointing to `start.bat service`
- Starts the service
- Reports status and service details including version being used

## Service Configuration

**Service Name:** `WedKnots`  
**Display Name:** `WedKnots Application Service`  
**Startup Type:** `Automatic`  
**Binary Path:** `cmd.exe /c "<path-to-latest-version>\bin\start.bat" service`  

## Practical Examples

### Example 1: Deploy and Install Latest Version
```bat
REM Deploy version 1.0.3
release-deploy.bat 1.0.3

REM Install service (will auto-detect 1.0.3 as latest)
install-service.bat

REM Verify
sc query WedKnots
```

### Example 2: Maintain Specific Version
```bat
REM Keep service on version 1.0.2 even if 1.0.3 is deployed
install-service.bat 1.0.2

REM Verify which version is running
sc query WedKnots
```

### Example 3: Switch Between Versions
```bat
REM Currently running 1.0.2
sc query WedKnots

REM Deploy version 1.0.3
release-deploy.bat 1.0.3

REM Switch service to new version
install-service.bat 1.0.3

REM Verify new version is running
sc query WedKnots
```

### Example 4: Rollback to Previous Version
```bat
REM Service is on 1.0.3 but something is wrong
REM Rollback to 1.0.2
install-service.bat 1.0.2

REM Service now runs 1.0.2 again
sc query WedKnots
```

## Troubleshooting

### "Administrator Privileges Required"

### Check Service Status
```bat
sc query WedKnots
```

### View Service Details
```bat
sc qc WedKnots
```

### Open Services Manager
```bat
services.msc
```

### Check Application Logs
```bat
type "C:\hosting\wedknots\wed-knots-1.0.3\logs\wedknots.log"
```

## Troubleshooting

### "Administrator Privileges Required"
- Right-click on Command Prompt
- Select "Run as administrator"
- Navigate to scripts folder
- Run `install-service.bat`

### "Hosting folder not found"
- Verify `C:\hosting\wedknots` exists
- Check folder structure with `dir C:\hosting\wedknots`
- Deploy application using release-deploy.bat first

### "No wed-knots version folders found"
- Ensure application is deployed: `release-deploy.bat 1.0.2`
- Check folder names match pattern `wed-knots-*` (case-sensitive)

### Service created but not running
- Check application log: `wedknots.log`
- Verify `start.bat` exists in bin folder
- Check database connection in config.env
- Run `start.bat` manually to see errors

### Need to switch to different version
Simply run `install-service.bat` again. It will:
1. Stop current service
2. Detect the newest version
3. Install and start the new version

## Integration with Release Workflow

**Deploy new version:**
```bat
release-deploy.bat 1.0.3
```

**Install as service (auto-detect latest):**
```bat
install-service.bat
```

**Install as service (specific version):**
```bat
install-service.bat 1.0.3
```

**Remove service:**
```bat
install-service.bat /uninstall
```

**Complete deployment and service update workflow:**
```bat
REM 1. Build, package and deploy new version
release-deploy.bat 1.0.3

REM 2. Install service with the newly deployed version
install-service.bat 1.0.3

REM 3. Verify service is running
sc query WedKnots
```

## Related Scripts

- **start.bat** - Application startup script (called by service)
- **release-deploy.bat** - Build, package, and deploy new versions
- **stop.bat** - Stop the application

## Requirements

- Windows 7 or later
- Administrator privileges
- PowerShell 3.0 or later
- Java installed and in PATH
- `C:\hosting\wedknots` directory with deployed application

## Notes

- Service runs with `Automatic` startup type (starts on boot)
- The service monitors the Java process and keeps it running
- Service logs to application's `logs/wedknots.log` file
- To view real-time logs: `tail -f C:\hosting\wedknots\wed-knots-1.0.3\logs\wedknots.log`

