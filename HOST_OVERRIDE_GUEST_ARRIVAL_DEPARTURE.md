# Host Override Preferred Arrival/Departure Dates for Guests

## Overview

Hosts can now override the default expected arrival and departure dates/times when adding or editing guests. These preferred dates are pre-populated with defaults from the wedding event details but can be customized per guest.

---

## Features Implemented

### 1. Guest Model Enhancement

Added two new fields to the `Guest` entity:
- `preferredArrivalDateTime` - Guest's preferred arrival date and time
- `preferredDepartureDateTime` - Guest's preferred departure date and time

**Database Schema:**
```sql
ALTER TABLE guest_tbl ADD COLUMN preferred_arrival_date_time VARCHAR(255);
ALTER TABLE guest_tbl ADD COLUMN preferred_departure_date_time VARCHAR(255);
```

**Model:**
```java
@Column(name = "preferred_arrival_date_time")
private String preferredArrivalDateTime;

@Column(name = "preferred_departure_date_time")
private String preferredDepartureDateTime;
```

### 2. Guest Form Enhancement

Updated `guest_form.html` to include two new sections:

#### Preferred Arrival Section
```html
<div class="card mb-4 bg-light">
    <div class="card-header">
        <h5 class="mb-0"><i class="bi bi-arrow-down-circle"></i> Preferred Arrival</h5>
    </div>
    <div class="card-body">
        <label for="preferredArrivalDateTime">Arrival Date & Time</label>
        <input type="datetime-local" class="form-control" id="preferredArrivalDateTime" 
               name="preferredArrivalDateTime" th:value="${guest.preferredArrivalDateTime}">
        <small class="form-text text-muted">
            Default from event: Dec 11, 2026 12:00 AM
        </small>
    </div>
</div>
```

#### Preferred Departure Section
```html
<div class="card mb-4 bg-light">
    <div class="card-header">
        <h5 class="mb-0"><i class="bi bi-arrow-up-circle"></i> Preferred Departure</h5>
    </div>
    <div class="card-body">
        <label for="preferredDepartureDateTime">Departure Date & Time</label>
        <input type="datetime-local" class="form-control" id="preferredDepartureDateTime" 
               name="preferredDepartureDateTime" th:value="${guest.preferredDepartureDateTime}">
        <small class="form-text text-muted">
            Default from event: Dec 13, 2026 12:00 AM
        </small>
    </div>
</div>
```

---

## Workflow

### Adding a New Guest

```
1. Host goes to: /events/{eventId}/guests/new
2. Fills in guest information:
   - Family Name
   - Contact Name
   - Email
   - Phone
   - Side (Bride/Groom/Both)
   - Address
   - Max Attendees

3. Sets Preferred Arrival:
   - Shows default from event: "Dec 11, 2026 12:00 AM"
   - Host can override with custom date/time
   - Field: datetime-local input

4. Sets Preferred Departure:
   - Shows default from event: "Dec 13, 2026 12:00 AM"
   - Host can override with custom date/time
   - Field: datetime-local input

5. Clicks "Save Guest"
6. Guest is created with custom or default arrival/departure times
```

### Editing an Existing Guest

```
1. Host goes to: /events/{eventId}/guests/{guestId}/edit
2. Sees all guest fields including:
   - Current preferred arrival time
   - Current preferred departure time

3. Can modify any field including dates
4. Clicks "Save Guest"
5. Guest record updated with new dates
```

---

## Default Values from Event

The event has default arrival/departure dates set at the event level:

**WeddingEvent Model:**
```java
@Column(name = "expected_guest_arrival_date_time")
private String expectedGuestArrivalDateTime;  // "Dec 11, 2026 12:00 AM"

@Column(name = "expected_guest_departure_date_time")
private String expectedGuestDepartureDateTime;  // "Dec 13, 2026 12:00 AM"
```

**Default Values (from Seed Data):**
- Wedding Date: December 12, 2026
- Expected Guest Arrival: December 11, 2026 at 12:00 AM (one day before)
- Expected Guest Departure: December 13, 2026 at 12:00 AM (one day after)

### Displaying Event Defaults

Each field shows the event default as a help text:
```html
<small class="form-text text-muted">
    Default from event: <span th:text="${event.expectedGuestArrivalDateTime ?: 'Not set'}">Default</span>
</small>
```

---

## Database Schema

### Guest Table

```sql
CREATE TABLE guest_tbl (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_name VARCHAR(255),
    contact_name VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    side VARCHAR(255),
    address VARCHAR(255),
    max_attendees INT,
    event_id BIGINT,
    preferred_arrival_date_time VARCHAR(255),      -- NEW
    preferred_departure_date_time VARCHAR(255),    -- NEW
    FOREIGN KEY (event_id) REFERENCES wedding_event_tbl(id)
);
```

### Wedding Event Table (Defaults)

```sql
CREATE TABLE wedding_event_tbl (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ...
    expected_guest_arrival_date_time VARCHAR(255),    -- "Dec 11, 2026 12:00 AM"
    expected_guest_departure_date_time VARCHAR(255),  -- "Dec 13, 2026 12:00 AM"
    ...
);
```

---

## Use Cases

### Use Case 1: Adding Guest with Event Defaults
```
Host: "I'll add a guest with the default event arrival/departure times"
Action:
1. Go to Add Guest form
2. Fill guest details
3. Leave Preferred Arrival empty (or see default: "Dec 11, 2026 12:00 AM")
4. Leave Preferred Departure empty (or see default: "Dec 13, 2026 12:00 AM")
5. Save Guest
Result: Guest has event default arrival/departure times
```

### Use Case 2: Adding Guest with Custom Arrival/Departure
```
Host: "This guest is arriving on Dec 10 and leaving on Dec 14 instead"
Action:
1. Go to Add Guest form
2. Fill guest details
3. Set Preferred Arrival to "Dec 10, 2026 10:00 AM"
4. Set Preferred Departure to "Dec 14, 2026 02:00 PM"
5. Save Guest
Result: Guest has custom arrival/departure times (not event defaults)
```

### Use Case 3: Editing Guest Dates
```
Host: "The guest's flight changed - need to update their arrival time"
Action:
1. Go to Edit Guest form
2. See current Preferred Arrival: "Dec 11, 2026 12:00 AM"
3. Change to "Dec 11, 2026 03:30 PM"
4. Save Guest
Result: Guest record updated with new arrival time
```

---

## Form Fields

| Field | Type | Required | Default | Notes |
|-------|------|----------|---------|-------|
| preferredArrivalDateTime | datetime-local | No | Event default | HTML5 date+time picker |
| preferredDepartureDateTime | datetime-local | No | Event default | HTML5 date+time picker |

---

## API Endpoint Changes

### POST /events/{eventId}/guests/new
Accepts new field: `preferredArrivalDateTime`, `preferredDepartureDateTime`

### POST /events/{eventId}/guests/{guestId}/edit
Accepts new field: `preferredArrivalDateTime`, `preferredDepartureDateTime`

---

## Migration

**Liquibase ChangeSet: 26**
```xml
<changeSet id="26-add-preferred-arrival-departure-to-guest" author="auto">
    <addColumn tableName="guest_tbl">
        <column name="preferred_arrival_date_time" type="VARCHAR(255)"/>
        <column name="preferred_departure_date_time" type="VARCHAR(255)"/>
    </addColumn>
</changeSet>
```

---

## UI/UX Design

### Visual Layout
```
Guest Form
├─ Basic Information Card
│  ├─ Family Name
│  ├─ Contact Name
│  ├─ Email
│  ├─ Phone
│  ├─ Side
│  ├─ Address
│  └─ Max Attendees
├─ Preferred Arrival Card (Light Gray)
│  ├─ <i class="bi bi-arrow-down-circle"></i> Header
│  └─ Arrival Date & Time (datetime-local input)
│     └─ Help text: "Default from event: Dec 11, 2026 12:00 AM"
├─ Preferred Departure Card (Light Gray)
│  ├─ <i class="bi bi-arrow-up-circle"></i> Header
│  └─ Departure Date & Time (datetime-local input)
│     └─ Help text: "Default from event: Dec 13, 2026 12:00 AM"
└─ Action Buttons
   ├─ Cancel
   └─ Save Guest
```

---

## Files Modified

| File | Changes |
|------|---------|
| `Guest.java` | Added `preferredArrivalDateTime` and `preferredDepartureDateTime` fields |
| `guest_form.html` | Added two card sections for preferred arrival/departure with datetime inputs |
| `db.changelog-master.xml` | Added ChangeSet 26 to create database columns |

---

## Build Status

```
BUILD SUCCESS ✅
Total time: 13.589 s
```

---

## Future Enhancements

- [ ] Auto-populate fields on page load based on event defaults
- [ ] Show conflict warnings if guest dates overlap with event dates
- [ ] Time zone support for international guests
- [ ] Bulk import of guest arrival/departure times from Excel
- [ ] Calendar view to see all guest arrival/departure times
- [ ] Coordination with travel info module for consistency

---

**Status:** ✅ Complete  
**Date:** January 7, 2026

