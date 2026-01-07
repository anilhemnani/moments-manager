re# Seed Data Update - Event Arrival/Departure and Airport/Station Details

## Summary

Updated the seed data in the database migration to set the following event details for the Pratibha & Karthik Wedding:

### Updated Event Details

| Field | Value |
|-------|-------|
| **Expected Guest Arrival** | 11/12/2026 9:00 AM |
| **Expected Guest Departure** | 13/12/2026 12:00 PM |
| **Preferred Airport** | Mangluru |
| **Preferred Station** | Kasaragod |

---

## Changes Made

### Database Migration (ChangeSet 10)

**File:** `src/main/resources/db/changelog/db.changelog-master.xml`

**Before:**
```xml
<update tableName="wedding_event_tbl">
    <column name="expected_guest_arrival_date_time" value="Dec 11, 2026 12:00 AM"/>
    <column name="expected_guest_departure_date_time" value="Dec 13, 2026 12:00 AM"/>
    <where>id = 1</where>
</update>
```

**After:**
```xml
<update tableName="wedding_event_tbl">
    <column name="expected_guest_arrival_date_time" value="11/12/2026 9:00 AM"/>
    <column name="expected_guest_departure_date_time" value="13/12/2026 12:00 PM"/>
    <column name="preferred_airport_arrival" value="Mangluru"/>
    <column name="preferred_station_arrival" value="Kasaragod"/>
    <where>id = 1</where>
</update>
```

---

## Event Details

### Wedding Event: Pratibha Hemnani & Karthik Puravankara Wedding

**Event ID:** 1

**Basic Details:**
- Bride: Pratibha
- Groom: Karthik
- Wedding Date: 12/12/2026
- Subdomain: pratibha-karthik
- Status: Published

**Travel Details (Updated):**
- **Expected Arrival Date/Time:** 11/12/2026 9:00 AM (one day before wedding)
- **Expected Departure Date/Time:** 13/12/2026 12:00 PM (one day after wedding)
- **Preferred Airport:** Mangluru (Mangaluru International Airport)
- **Preferred Station:** Kasaragod

---

## Impact on Guest Experience

### When Guests Update RSVP

Guests will now see the updated expected travel schedule:

```
┌─ Expected Travel Schedule ─────────────────┐
│                                            │
│  ⬇️ Arrival                    ⬆️ Departure │
│  11/12/2026 9:00 AM            13/12/2026  │
│  ✈️ Mangluru                   12:00 PM    │
│                                🚂 Kasaragod │
│                                            │
└────────────────────────────────────────────┘
```

### When Guests Update Travel Details

Travel info form will pre-populate with:
- **Arrival Date/Time:** 11/12/2026 9:00 AM (if no guest preferred date)
- **Arrival Airport:** Mangluru (can be overridden)
- **Departure Date/Time:** 13/12/2026 12:00 PM (if no guest preferred date)
- **Departure Station:** Kasaragod (can be overridden)

---

## Data Flow

### Guest with No Preferred Dates (Host Default)
```
Guest RSVP Form:
├─ Arrival: 11/12/2026 9:00 AM (event default)
└─ Departure: 13/12/2026 12:00 PM (event default)

Travel Info Form (Pre-population):
├─ Arrival Date/Time: 11/12/2026 9:00 AM (event default)
├─ Arrival Airport: Mangluru (event default)
├─ Departure Date/Time: 13/12/2026 12:00 PM (event default)
└─ Departure Station: Kasaragod (event default)
```

### Guest with Custom Host-Set Dates
```
Host sets Guest Arrival: 10/12/2026 6:00 PM (custom)
Guest sets Guest Departure: 13/12/2026 12:00 PM (default)

Guest RSVP Form:
├─ Arrival: 10/12/2026 6:00 PM (guest preferred)
└─ Departure: 13/12/2026 12:00 PM (event default)

Travel Info Form:
├─ Arrival Date/Time: 10/12/2026 6:00 PM (guest preferred)
├─ Arrival Airport: Mangluru (event default)
├─ Departure Date/Time: 13/12/2026 12:00 PM (event default)
└─ Departure Station: Kasaragod (event default)
```

---

## Guest Information Updated

The following guests will see these defaults:

| Guest ID | Family Name | Contact Name | Default Arrival | Default Departure |
|----------|------------|--------------|-----------------|-------------------|
| 1 | Sharma | Ravi Sharma | 11/12/2026 9:00 AM | 13/12/2026 12:00 PM |
| 2 | Patel | Meera Patel | 11/12/2026 9:00 AM | 13/12/2026 12:00 PM |

**Note:** Guests can have their own preferred dates set by the host when adding them, which will override these event defaults.

---

## Related Configuration

### Event Travel Details (Now Set)
- ✅ Expected Guest Arrival Date/Time
- ✅ Expected Guest Departure Date/Time
- ✅ Preferred Airport Arrival
- ✅ Preferred Station Arrival

### Host-Level Guest Customization
Hosts can override these event defaults for individual guests:
- Preferred Arrival Date/Time
- Preferred Departure Date/Time

### Guest-Level Customization
Guests can further override defaults in Travel Info:
- Arrival Mode (Flight/Train/Car/Bus)
- Arrival Date/Time
- Arrival Flight/Train Number
- Arrival Airport/Station
- Departure Mode (Flight/Train/Car/Bus)
- Departure Date/Time
- Departure Flight/Train Number
- Departure Airport/Station
- Pickup/Drop requirements
- Special requirements
- Notes

---

## Database Update

**Table:** `wedding_event_tbl`  
**Record ID:** 1

**Updated Columns:**
```sql
UPDATE wedding_event_tbl 
SET 
    expected_guest_arrival_date_time = '11/12/2026 9:00 AM',
    expected_guest_departure_date_time = '13/12/2026 12:00 PM',
    preferred_airport_arrival = 'Mangluru',
    preferred_station_arrival = 'Kasaragod'
WHERE id = 1;
```

---

## Build Status

```
BUILD SUCCESS ✅
Total time: 14.361 s
Finished at: 2026-01-07T19:58:16Z
```

---

## Testing

To verify the changes:

1. **Start the application**
   - Database migrations will run automatically
   - Event will be created with updated arrival/departure times and airport/station

2. **Login as Guest**
   - Phone: +447878597720
   - Family Name: Sharma or Patel

3. **View Invitation**
   - Click "Update RSVP"
   - Should see Expected Travel Schedule with:
     - Arrival: 11/12/2026 9:00 AM
     - Airport: Mangluru
     - Departure: 13/12/2026 12:00 PM
     - Station: Kasaragod

4. **Update Travel Details**
   - Fields should pre-populate with event defaults
   - Can override with custom values

---

**Status:** ✅ Complete  
**Date:** January 7, 2026

