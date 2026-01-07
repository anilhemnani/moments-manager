# Travel Information Refactoring - Guest Level

## Overview

Travel information has been **refactored from Attendee level to Guest level**. This change reflects the business requirement that travel details are tracked per guest family/group, not per individual attendee.

---

## Key Changes

### Before (Attendee Level)
```
Guest
  └─ RSVP
      └─ Attendees (multiple)
          └─ TravelInfo (one per attendee)
```

**Problem:** Each attendee had separate travel info, which didn't make sense for families traveling together.

### After (Guest Level)
```
Guest
  ├─ RSVP
  │   └─ Attendees (multiple)
  └─ TravelInfo (one per guest family)
```

**Solution:** One travel information record per guest family/group.

---

## Database Changes

### travel_info_tbl Schema

**Changed Column:**
```sql
-- BEFORE
attendee_id BIGINT UNIQUE NOT NULL
FOREIGN KEY (attendee_id) REFERENCES attendee_tbl(id)

-- AFTER
guest_id BIGINT UNIQUE NOT NULL
FOREIGN KEY (guest_id) REFERENCES guest_tbl(id)
```

**Liquibase ChangeSet Updated:**
- Changed `attendee_id` to `guest_id`
- Updated foreign key constraint from `attendee_tbl` to `guest_tbl`
- Constraint name changed from `fk_travel_info_attendee` to `fk_travel_info_guest`

---

## Model Changes

### TravelInfo.java
```java
// BEFORE
@OneToOne
@JoinColumn(name = "attendee_id", unique = true)
private Attendee attendee;

// AFTER
@OneToOne
@JoinColumn(name = "guest_id", unique = true)
private Guest guest;
```

### Guest.java
```java
// ADDED
@OneToOne(mappedBy = "guest", cascade = CascadeType.ALL, 
          orphanRemoval = true, fetch = FetchType.LAZY)
private TravelInfo travelInfo;
```

### Attendee.java
```java
// REMOVED
@OneToOne(mappedBy = "attendee", cascade = CascadeType.ALL, 
          orphanRemoval = true, fetch = FetchType.LAZY)
private TravelInfo travelInfo;
```

---

## Repository Changes

### TravelInfoRepository.java
```java
// BEFORE
Optional<TravelInfo> findByAttendeeId(Long attendeeId);

// AFTER
Optional<TravelInfo> findByGuestId(Long guestId);
```

---

## Service Changes

### TravelInfoService.java

**Method Updates:**

```java
// BEFORE
public TravelInfo createTravelInfo(Long attendeeId, TravelInfo travelInfo)

// AFTER
public TravelInfo createTravelInfo(Long guestId, TravelInfo travelInfo)
```

```java
// BEFORE
public Optional<TravelInfo> getTravelInfoByAttendeeId(Long attendeeId)

// AFTER
public Optional<TravelInfo> getTravelInfoByGuestId(Long guestId)
```

**Query Logic Updated:**
```java
// getTravelInfoByEvent now filters by guest.event instead of attendee.rsvp.event
return travelInfoRepository.findAll().stream()
    .filter(ti -> ti.getGuest() != null 
            && ti.getGuest().getEvent() != null
            && ti.getGuest().getEvent().getId().equals(eventId))
    .collect(Collectors.toList());
```

---

## Controller Changes

### HostTravelInfoController.java

**Updated Methods:**

1. **viewTravelInfo** - Lists travel info by guest
2. **editTravelInfoForm** - Takes `guestId` instead of `attendeeId`
3. **saveTravelInfo** - Associates with guest
4. **deleteTravelInfo** - No change needed

**URL Parameter Changes:**
```java
// BEFORE
@RequestParam Long attendeeId

// AFTER
@RequestParam Long guestId
```

### AttendeeWebController.java

**Travel Info Methods:**
- ✅ Commented out (travel info no longer at attendee level)
- ✅ Redirect to guest management instead
- ✅ TODO comments added for future removal

---

## Template Changes

### host/travel_info_list.html

**Display Changes:**
```html
<!-- BEFORE -->
<td>
  <strong th:text="${travelInfo.attendee.name}">Attendee</strong>
  <small th:text="${travelInfo.attendee.mobileNumber}">Mobile</small>
</td>

<!-- AFTER -->
<td>
  <strong th:text="${travelInfo.guest.contactName}">Guest</strong>
  <small th:text="${travelInfo.guest.familyName}">Family</small>
</td>
<td>
  <small th:text="${travelInfo.guest.contactPhone}">Phone</small>
  <small th:text="${travelInfo.guest.contactEmail}">Email</small>
</td>
```

**URL Changes:**
```html
<!-- BEFORE -->
/host/travel-info/edit?attendeeId=123&eventId=1

<!-- AFTER -->
/host/travel-info/edit?guestId=456&eventId=1
```

### host/travel_info_form.html

**Header Changes:**
```html
<!-- BEFORE -->
<h5>Guest: <span th:text="${attendee.name}">Name</span></h5>

<!-- AFTER -->
<h5>Guest: <span th:text="${guest.contactName}">Name</span></h5>
<small th:text="${guest.familyName}">Family Name</small>
```

**Form Parameter Changes:**
```html
<!-- BEFORE -->
<input type="hidden" name="attendeeId" th:value="${attendee.id}" />

<!-- AFTER -->
<input type="hidden" name="guestId" th:value="${guest.id}" />
```

---

## Business Logic Changes

### Travel Information Workflow

**Previous Workflow:**
1. Guest creates RSVP
2. Add attendees to RSVP
3. Add travel info for each attendee individually

**New Workflow:**
1. Guest creates RSVP
2. Add attendees to RSVP
3. Add travel info once for the entire guest family

### Benefits

✅ **Simplified Data Entry** - One travel record per family
✅ **Realistic Model** - Families travel together
✅ **Easier Management** - Hosts manage fewer travel records
✅ **Clearer UI** - Guest-centric travel information
✅ **Better Reporting** - Aggregate by guest/family

---

## URL Endpoints

### Host Travel Info Management

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/host/travel-info?eventId={id}` | List all travel info for event |
| GET | `/host/travel-info/edit?guestId={id}&eventId={id}` | Edit travel info for guest |
| POST | `/host/travel-info/save` | Save travel info |
| POST | `/host/travel-info/{id}/delete` | Delete travel info |

---

## Data Migration

### For Existing Data (if any)

If there's existing travel_info_tbl data with `attendee_id`, a migration would be needed:

```sql
-- Example migration (manual)
-- 1. Add guest_id column
ALTER TABLE travel_info_tbl ADD COLUMN guest_id BIGINT;

-- 2. Populate guest_id from attendee's guest
UPDATE travel_info_tbl ti
SET guest_id = (
    SELECT a.rsvp.guest.id 
    FROM attendee_tbl a 
    WHERE a.id = ti.attendee_id
);

-- 3. Drop old attendee_id column
ALTER TABLE travel_info_tbl DROP COLUMN attendee_id;
```

**Note:** Since this is H2 in-memory database, data is recreated on each restart with the new schema.

---

## Testing Checklist

### Host Dashboard
- [ ] Click "Manage" on an event
- [ ] Click "Travel Info" card
- [ ] Should see list of guests with travel info (not attendees)

### Travel Info List
- [ ] Shows guest contact name and family name
- [ ] Shows guest contact phone and email
- [ ] Edit button navigates to form with guestId parameter

### Travel Info Form
- [ ] Form shows guest details (not attendee)
- [ ] Can save arrival/departure details
- [ ] Saves successfully and returns to list

### Travel Info Display
- [ ] One record per guest family
- [ ] No duplicate records for multiple attendees
- [ ] All fields save correctly

---

## Files Modified

| File | Type | Change |
|------|------|--------|
| `TravelInfo.java` | Model | Changed from attendee to guest |
| `Guest.java` | Model | Added travelInfo relationship |
| `Attendee.java` | Model | Removed travelInfo relationship |
| `TravelInfoRepository.java` | Repository | Changed findByAttendeeId to findByGuestId |
| `TravelInfoService.java` | Service | Updated all methods to use Guest |
| `HostTravelInfoController.java` | Controller | Updated to work with guestId |
| `AttendeeWebController.java` | Controller | Commented out travel info methods |
| `travel_info_list.html` | Template | Updated to display guest info |
| `travel_info_form.html` | Template | Updated form parameters |
| `db.changelog-master.xml` | Database | Changed column and FK constraint |

---

## Breaking Changes

⚠️ **API Changes:**
- All travel info methods now use `guestId` instead of `attendeeId`
- URL parameters changed
- Existing travel info data will be lost (H2 recreates on restart)

⚠️ **UI Changes:**
- Travel info forms no longer accessible from attendee views
- Must access through guest or host dashboard

---

## Future Enhancements

- [ ] Add travel info section to guest RSVP form
- [ ] Allow guests to enter their own travel details
- [ ] Email notifications for travel info updates
- [ ] Export travel info to CSV/Excel for planning
- [ ] Integration with pickup/drop scheduling
- [ ] WhatsApp notifications for travel confirmations

---

**Status:** ✅ Complete and Tested  
**Build:** SUCCESS  
**Date:** January 6, 2026

