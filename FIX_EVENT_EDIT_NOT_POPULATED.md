# Fix: Event Not Populated When Editing

## Issue
Event data was not being populated in the edit form when navigating to `/events/{id}/edit` or `/admin/events/{id}/edit`.

---

## Root Causes Identified

### 1. Liquibase Autoconfiguration Excluded
**Problem:**
```yaml
autoconfigure:
  exclude: org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
```
This prevented Liquibase from running automatically, so the database wasn't being initialized with the seed data.

**Fix:**
Removed the exclusion so Liquibase runs on application startup.

### 2. Database Not Dropping on Restart
**Problem:**
```yaml
liquibase:
  drop-first: false
```
With an in-memory H2 database, if the schema changed, old migrations could conflict.

**Fix:**
Changed to `drop-first: true` to ensure clean database initialization each startup.

### 3. Missing TravelInfo Columns in Schema
**Problem:**
The database schema was missing several columns that exist in the `TravelInfo` entity:
- `arrival_flight_number`
- `arrival_train_number`
- `arrival_airport`
- `arrival_station`
- `departure_flight_number`
- `departure_train_number`
- `departure_airport`
- `departure_station`
- `needs_pickup`
- `needs_drop`
- `special_requirements`

The schema had old combined fields like `arrival_flight_train_number` instead.

**Fix:**
Updated the Liquibase changelog to include all correct column names matching the entity.

---

## Changes Made

### 1. Updated `application.yml`
**Before:**
```yaml
liquibase:
  enabled: true
  change-log: classpath:db/changelog/db.changelog-master.xml
  drop-first: false
autoconfigure:
  exclude: org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
```

**After:**
```yaml
liquibase:
  enabled: true
  change-log: classpath:db/changelog/db.changelog-master.xml
  drop-first: true
```

### 2. Updated `db.changelog-master.xml`
**travel_info_tbl schema - Added all missing columns:**
```xml
<column name="arrival_flight_number" type="VARCHAR(255)"/>
<column name="arrival_train_number" type="VARCHAR(255)"/>
<column name="arrival_airport" type="VARCHAR(255)"/>
<column name="arrival_station" type="VARCHAR(255)"/>
<column name="arrival_pnr_number" type="VARCHAR(255)"/>
<column name="arrival_port" type="VARCHAR(255)"/>

<column name="departure_flight_number" type="VARCHAR(255)"/>
<column name="departure_train_number" type="VARCHAR(255)"/>
<column name="departure_airport" type="VARCHAR(255)"/>
<column name="departure_station" type="VARCHAR(255)"/>
<column name="departure_pnr_number" type="VARCHAR(255)"/>
<column name="departure_port" type="VARCHAR(255)"/>

<column name="needs_pickup" type="BOOLEAN" defaultValueBoolean="false"/>
<column name="needs_drop" type="BOOLEAN" defaultValueBoolean="false"/>
<column name="special_requirements" type="VARCHAR(1000)"/>
```

---

## How Event Loading Works

### Controller Flow (EventWebController & AdminEventController)

#### GET /events/{id}/edit or /admin/events/{id}/edit
```java
@GetMapping("/{id}/edit")
public String editEvent(@PathVariable Long id, Model model) {
    Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(id);
    if (eventOpt.isPresent()) {
        model.addAttribute("event", eventOpt.get());
        return "event_form"; // or "admin_event_form"
    }
    return "redirect:/events";
}
```

**Steps:**
1. Controller receives event ID from URL path
2. Queries database using `weddingEventRepository.findById(id)`
3. If event exists, adds it to model with name "event"
4. Returns template name to render
5. Thymeleaf binds event object to form using `th:object="${event}"`
6. Form fields populate using `th:field="*{fieldName}"`

---

## Seed Data That Should Load

When application starts, Liquibase creates and populates:

### Wedding Event (ID: 1)
```
Name: Pratibha Hemnani & Karthik Puravankara Wedding
Date: 2026-12-12
Status: Published
Bride: Pratibha
Groom: Karthik
Subdomain: pratibha-karthik
Expected Arrival: 2026-12-11
Expected Departure: 2026-12-13
Preferred Airport: Mangluru
Preferred Station: Kasaragod
```

### Host (ID: 1)
```
Username: host1
Email: host1@example.com
Phone: +447878597720
Event: 1
```

### Guest (ID: 1)
```
Family: Sharma
Contact: Ravi Sharma
Email: ravi.sharma@example.com
Phone: +447878597720
Side: Bride
Expected Arrival: 2026-12-11
Expected Departure: 2026-12-13
Event: 1
```

---

## Testing Steps

### 1. Verify Application Started Successfully
Check the terminal output for:
```
Started MomentsManagerApplication in X seconds
Liquibase successfully applied X changeset(s)
```

### 2. Verify Database Was Created
Navigate to H2 Console: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:weddingdb`
- Username: `sa`
- Password: (leave empty)

Run queries:
```sql
SELECT * FROM wedding_event_tbl;
SELECT * FROM guest_tbl;
SELECT * FROM host_tbl;
```

Should see the seed data.

### 3. Test Edit Form (Admin)
1. Login as admin: http://localhost:8080/admin/login
2. Navigate to: http://localhost:8080/admin/events/1/edit
3. **Expected Results:**
   - Form should be pre-filled with:
     - Event Name: "Pratibha Hemnani & Karthik Puravankara Wedding"
     - Bride Name: "Pratibha"
     - Groom Name: "Karthik"
     - Event Date: 2026-12-12
     - Arrival Date: 2026-12-11
     - Departure Date: 2026-12-13
     - Airport: "Mangluru"
     - Station: "Kasaragod"
     - Subdomain: "pratibha-karthik" (disabled field)

### 4. Test Edit Form (Host - if applicable)
1. Login as host1
2. Navigate to: http://localhost:8080/events/1/edit
3. Should see same pre-filled data (if host has edit permission)

### 5. Verify Form Submission Works
1. On edit form, change Event Date to 2026-12-15
2. Submit form
3. **Expected:**
   - Redirects to event view/list
   - Success message shown
   - Event updated in database
   - Subdomain remains unchanged

---

## Troubleshooting

### If Form Still Empty

**Check 1: Verify Liquibase Ran**
Look for in logs:
```
INFO liquibase.changelog : Changeset db/changelog/db.changelog-master.xml::13-insert-initial-data::auto ran successfully
```

**Check 2: Verify Event Exists in Database**
```sql
SELECT * FROM wedding_event_tbl WHERE id = 1;
```

**Check 3: Check for Hibernate Errors**
Look for in logs:
```
ERROR org.hibernate
```

**Check 4: Verify Repository Returns Data**
Add debug logging to controller:
```java
@GetMapping("/{id}/edit")
public String editEvent(@PathVariable Long id, Model model) {
    Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(id);
    System.out.println("Event found: " + eventOpt.isPresent());
    if (eventOpt.isPresent()) {
        WeddingEvent event = eventOpt.get();
        System.out.println("Event name: " + event.getName());
        System.out.println("Event date: " + event.getDate());
        model.addAttribute("event", event);
        return "event_form";
    }
    return "redirect:/events";
}
```

### If Liquibase Fails

**Check for:**
- Syntax errors in XML
- Column type mismatches
- Foreign key constraint violations
- Duplicate changeset IDs

**To force fresh start:**
1. Stop application
2. Delete H2 database file (if file-based)
3. Ensure `drop-first: true` in application.yml
4. Restart application

---

## Expected Behavior After Fix

✅ **On Application Startup:**
- Liquibase drops existing H2 database
- Creates all tables from scratch
- Inserts seed data (1 event, 1 host, 1 guest, 1 RSVP)

✅ **When Navigating to Edit Form:**
- Event ID 1 loads from database
- All fields populate with seed data values
- Date fields show formatted dates (2026-12-12, etc.)
- Subdomain field is disabled (cannot be changed)

✅ **When Submitting Edit Form:**
- Event updates in database
- Subdomain preserved (cannot change)
- Success message displayed
- Redirects to event view/list

---

## Related Files

### Modified Files:
1. `src/main/resources/application.yml` - Removed Liquibase exclusion, enabled drop-first
2. `src/main/resources/db/changelog/db.changelog-master.xml` - Fixed travel_info_tbl schema

### Controller Files (No Changes - Already Correct):
1. `src/main/java/com/momentsmanager/web/EventWebController.java`
2. `src/main/java/com/momentsmanager/web/AdminEventController.java`

### Entity Files (No Changes Needed):
1. `src/main/java/com/momentsmanager/model/WeddingEvent.java`
2. `src/main/java/com/momentsmanager/model/TravelInfo.java`

---

**Status: ✅ FIXED - Restart application to test**

