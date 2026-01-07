# Database Initialization Fix - Complete Guide

## Issues Fixed

### 1. **Compilation Errors**
✅ **Fixed**: Removed outdated `@EntityScan` annotation from `MomentsManagerApplication.java`
- The annotation is not available in Spring Boot 4.0.1
- `@SpringBootApplication` handles entity scanning automatically

✅ **Fixed**: Updated Lombok from 1.18.26 to 1.18.30
- Version 1.18.26 was incompatible with JDK 21
- Caused `java.lang.NoSuchFieldError` during compilation

### 2. **Database Initialization**
✅ **Fixed**: Created `DatabaseInitializationConfig.java`
- Ensures Liquibase runs and creates database schema
- Manages initialization order properly

✅ **Fixed**: Updated `application.yml` configuration
- Set `hibernate.ddl-auto: none` to prevent conflicts
- Disabled auto-configured Liquibase (using custom bean instead)
- Set `defer-datasource-initialization: true`

### 3. **Security Updates**
✅ **Updated vulnerable libraries**:
- JJWT: 0.9.1 → 0.12.3 (modular: api, impl, jackson)
- SendGrid: 4.9.3 → 4.10.2
- Commons-IO: Added 2.15.1 for security

---

## How to Run the Application

### Method 1: Using Maven (Recommended for Development)
```powershell
cd C:\dev\projects\moments-manager
mvn clean spring-boot:run
```

### Method 2: Using JAR File
```powershell
cd C:\dev\projects\moments-manager
mvn clean package -DskipTests
java -jar target/moments-manager-0.0.1-SNAPSHOT.jar
```

### Method 3: Using IDE
- Open the project in IntelliJ IDEA or Eclipse
- Run `MomentsManagerApplication.java` as a Java Application

---

## Verifying Database Initialization

### 1. Check Application Startup Logs
Look for these log entries indicating successful initialization:
```
INFO liquibase.changelog : Creating database changelog table
INFO liquibase.changelog : Reading from PUBLIC.DATABASECHANGELOG
INFO liquibase.ui : Running Changeset: db/changelog/db.changelog-master.xml::1-create-role-table::auto
INFO liquibase.changelog : ChangeSet db/changelog/db.changelog-master.xml::7-insert-initial-data::auto ran successfully
```

### 2. Access H2 Console
Once the application is running:
1. Open browser: http://localhost:8080/h2-console
2. Connection details:
   - **JDBC URL**: `jdbc:h2:mem:weddingdb`
   - **Username**: `sa`
   - **Password**: (leave empty)
3. Click "Connect"

### 3. Verify Tables and Data
Run these SQL queries in H2 Console:

```sql
-- Check all tables
SHOW TABLES;

-- Verify seed data
SELECT * FROM role_tbl;
SELECT * FROM app_user_tbl;
SELECT * FROM wedding_event_tbl;
SELECT * FROM guest_tbl;
SELECT * FROM host_tbl;
SELECT * FROM rsvp_tbl;
```

Expected results:
- **3 roles**: ROLE_ADMIN, ROLE_HOST, ROLE_GUEST
- **3 users**: admin, host1, host2
- **1 wedding event**: "Ravi & Meera Wedding"
- **2 guests**: Sharma family, Patel family
- **2 hosts**: Host One, Host Two
- **2 RSVPs**: One for each guest

---

## Configuration Files

### application.yml (Key Settings)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:weddingdb;DB_CLOSE_DELAY=-1;MODE=MySQL
  jpa:
    hibernate:
      ddl-auto: none  # Let Liquibase handle schema
  liquibase:
    enabled: false    # Using custom bean instead
  h2:
    console:
      enabled: true
      path: /h2-console
server:
  port: 8080
```

### DatabaseInitializationConfig.java
- Custom Spring Bean that manages Liquibase execution
- Ensures proper initialization order
- Located at: `src/main/java/com/momentsmanager/config/`

### Liquibase Changelog
- Master file: `src/main/resources/db/changelog/db.changelog-master.xml`
- Creates all tables and inserts seed data
- Changesets include:
  - Tables: role, user, wedding_event, guest, host, rsvp, attendee, etc.
  - Seed data: Initial roles, users, and sample wedding data

---

## Troubleshooting

### Problem: Tables not created
**Solution**: Ensure `hibernate.ddl-auto` is set to `none` in application.yml

### Problem: Duplicate table error
**Solution**: This means Hibernate created tables before Liquibase. Restart the application - H2 in-memory database will be fresh.

### Problem: Application won't start
**Check**:
1. Port 8080 is not in use: `netstat -ano | findstr :8080`
2. Java 21 is installed: `java -version`
3. Build succeeded: `mvn clean package`

### Problem: No seed data
**Check**:
1. Liquibase executed: Look for "ChangeSet...ran successfully" in logs
2. H2 Console connection settings are correct
3. Database name matches: `weddingdb`

---

## Default Login Credentials

### Admin User
- **Username**: `admin`
- **Password**: (needs to be set on first login)
- **Email**: admin@moments-manager.com

### Host Users
- **Username**: `host1` or `host2`
- **Password**: (pre-hashed, needs reset)
- **Emails**: host1@example.com, host2@example.com

---

## Next Steps

1. ✅ Application compiles successfully
2. ✅ Database schema created via Liquibase
3. ✅ Seed data loaded
4. ✅ Security vulnerabilities fixed
5. ⏭️ Test the application features
6. ⏭️ Set up admin password
7. ⏭️ Configure WhatsApp integration (optional)

---

**Status**: All issues resolved ✅
**Last Updated**: January 5, 2026
**Build**: SUCCESS

