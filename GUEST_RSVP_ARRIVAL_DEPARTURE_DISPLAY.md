# Guest RSVP - Display Expected Arrival/Departure & Pre-populate Travel Info

## Overview

When guests update their RSVP, they now see:
1. **Expected Travel Schedule** - Arrival and departure dates/times based on their guest preferences or event defaults
2. **Preferred Airport/Station** - Shows the event's preferred arrival/departure locations
3. **Pre-populated Travel Info Form** - Travel details are pre-filled with guest's preferred dates and event defaults, which guests can override

---

## Features Implemented

### 1. RSVP Form Enhancement

Added an alert section showing expected travel schedule:

```html
<div class="alert alert-info mb-4" role="alert">
    <h6 class="alert-heading"><i class="bi bi-calendar-event"></i> Expected Travel Schedule</h6>
    <div class="row">
        <div class="col-md-6">
            <p class="mb-2">
                <strong><i class="bi bi-arrow-down-circle"></i> Arrival</strong><br>
                <span th:text="${guest.preferredArrivalDateTime ?: event.expectedGuestArrivalDateTime ?: 'Not set'}">
                    Arrival Date/Time
                </span>
            </p>
            <p class="mb-0" th:if="${event.preferredAirportArrival}">
                <small><i class="bi bi-airplane"></i> <span th:text="${event.preferredAirportArrival}">Airport</span></small>
            </p>
        </div>
        <div class="col-md-6">
            <p class="mb-2">
                <strong><i class="bi bi-arrow-up-circle"></i> Departure</strong><br>
                <span th:text="${guest.preferredDepartureDateTime ?: event.expectedGuestDepartureDateTime ?: 'Not set'}">
                    Departure Date/Time
                </span>
            </p>
            <p class="mb-0" th:if="${event.preferredStationArrival}">
                <small><i class="bi bi-train-front"></i> <span th:text="${event.preferredStationArrival}">Station</span></small>
            </p>
        </div>
    </div>
</div>
```

### 2. Travel Info Form Pre-population

Travel info form now pre-fills guest data with fallback to event defaults:

#### Arrival Date/Time
```html
<input type="datetime-local" th:field="*{arrivalDateTime}" class="form-control"
       th:value="${travelInfo.arrivalDateTime ?: guest.preferredArrivalDateTime ?: ''}">
<small class="form-text text-muted">
    Default: <span th:text="${guest.preferredArrivalDateTime ?: 'Not set'}">Default arrival</span>
</small>
```

#### Arrival Airport
```html
<input type="text" th:field="*{arrivalAirport}" class="form-control"
       placeholder="e.g., Indira Gandhi Airport"
       th:value="${travelInfo.arrivalAirport ?: event.preferredAirportArrival ?: ''}">
<small class="form-text text-muted">
    Default: <span th:text="${event.preferredAirportArrival ?: 'Not set'}">Default airport</span>
</small>
```

#### Departure Date/Time
```html
<input type="datetime-local" th:field="*{departureDateTime}" class="form-control"
       th:value="${travelInfo.departureDateTime ?: guest.preferredDepartureDateTime ?: ''}">
<small class="form-text text-muted">
    Default: <span th:text="${guest.preferredDepartureDateTime ?: 'Not set'}">Default departure</span>
</small>
```

#### Departure Airport
```html
<input type="text" th:field="*{departureAirport}" class="form-control"
       placeholder="e.g., Indira Gandhi Airport"
       th:value="${travelInfo.departureAirport ?: event.preferredAirportArrival ?: ''}">
<small class="form-text text-muted">
    Default: <span th:text="${event.preferredAirportArrival ?: 'Not set'}">Default airport</span>
</small>
```

---

## Data Sources and Fallback Chain

### Arrival Date/Time
```
Priority 1: TravelInfo.arrivalDateTime (if guest has already saved it)
Priority 2: Guest.preferredArrivalDateTime (host set when adding guest)
Priority 3: Event.expectedGuestArrivalDateTime (event default)
Fallback: "Not set" or empty
```

### Arrival Airport
```
Priority 1: TravelInfo.arrivalAirport (if guest has already saved it)
Priority 2: Event.preferredAirportArrival (event default)
Fallback: "Not set" or empty
```

### Departure Date/Time
```
Priority 1: TravelInfo.departureDateTime (if guest has already saved it)
Priority 2: Guest.preferredDepartureDateTime (host set when adding guest)
Priority 3: Event.expectedGuestDepartureDateTime (event default)
Fallback: "Not set" or empty
```

### Departure Airport
```
Priority 1: TravelInfo.departureAirport (if guest has already saved it)
Priority 2: Event.preferredAirportArrival (event default - reused for consistency)
Fallback: "Not set" or empty
```

---

## Guest Workflow

### Step 1: View Invitation
```
Guest clicks invitation → /invitations/{invitationId}
├─ Sees wedding details
├─ Sees RSVP Status section
├─ Sees Attendees section
└─ Sees Travel Information section
```

### Step 2: Update RSVP
```
Guest clicks "Update RSVP" → /invitations/rsvp/form?guestId=1&eventId=1
├─ Sees Guest Information
├─ Sees Expected Travel Schedule Alert:
│  ├─ Arrival: "Dec 11, 2026 12:00 AM" (from guest preferred or event default)
│  ├─ Arrival Airport: "Indira Gandhi Airport" (from event)
│  ├─ Departure: "Dec 13, 2026 12:00 AM"
│  └─ Departure Station: "Central Station"
├─ Selects RSVP Status (Accepted, Declined, Pending, Maybe)
├─ Enters Attendee Count
└─ Clicks "Update RSVP"
```

### Step 3: Update Travel Details
```
Guest clicks "Update Travel Details" → /invitations/travel-info?guestId=1&eventId=1
├─ Sees Arrival section with pre-filled data:
│  ├─ Mode: (empty, can select Flight/Train/Car/Bus)
│  ├─ Date/Time: "Dec 11, 2026 12:00 AM" (guest preferred or event default)
│  ├─ Flight/Train Number: (empty, optional)
│  └─ Airport: "Indira Gandhi Airport" (event default, can override)
├─ Sees Departure section with pre-filled data:
│  ├─ Mode: (empty, can select)
│  ├─ Date/Time: "Dec 13, 2026 12:00 AM"
│  ├─ Flight/Train Number: (empty, optional)
│  └─ Airport: "Indira Gandhi Airport"
├─ Can override any field
├─ Adds special requirements and notes
└─ Clicks "Save Travel Details"
```

---

## Controller Changes

### GuestInvitationsController Updates

**Added WeddingEventRepository injection:**
```java
@Autowired
private WeddingEventRepository weddingEventRepository;
```

**Updated rsvpForm method:**
- Now accepts `@RequestParam Long guestId` and `@RequestParam Long eventId`
- Fetches WeddingEvent and adds to model
- Guest form can display event defaults (expectedGuestArrivalDateTime, expectedGuestDepartureDateTime, preferredAirportArrival, preferredStationArrival)

**Updated travelInfoForm method:**
- Now accepts `@RequestParam Long guestId` and `@RequestParam Long eventId`
- Fetches WeddingEvent and adds to model
- Travel info form can pre-populate fields with guest preferred dates and event defaults

---

## Template Enhancements

### guest_rsvp_form.html

**New Section:** Expected Travel Schedule Alert
- Displays arrival date/time from guest.preferredArrivalDateTime or event.expectedGuestArrivalDateTime
- Shows preferred arrival airport/station if available
- Displays departure date/time from guest.preferredDepartureDateTime or event.expectedGuestDepartureDateTime
- Shows preferred departure airport/station if available

### guest_travel_info_form.html

**Enhanced Fields:**

| Field | Pre-population Source | Fallback |
|-------|----------------------|----------|
| Arrival Date/Time | travelInfo.arrivalDateTime → guest.preferredArrivalDateTime | Event default |
| Arrival Airport | travelInfo.arrivalAirport → event.preferredAirportArrival | Empty |
| Departure Date/Time | travelInfo.departureDateTime → guest.preferredDepartureDateTime | Event default |
| Departure Airport | travelInfo.departureAirport → event.preferredAirportArrival | Empty |

Each field shows its default value in help text:
```html
<small class="form-text text-muted">
    Default: <span th:text="${guest.preferredArrivalDateTime ?: 'Not set'}">Default arrival</span>
</small>
```

---

## Data Model References

### Guest Entity
```java
@Column(name = "preferred_arrival_date_time")
private String preferredArrivalDateTime;

@Column(name = "preferred_departure_date_time")
private String preferredDepartureDateTime;
```

### WeddingEvent Entity
```java
@Column(name = "expected_guest_arrival_date_time")
private String expectedGuestArrivalDateTime;  // "Dec 11, 2026 12:00 AM"

@Column(name = "expected_guest_departure_date_time")
private String expectedGuestDepartureDateTime;  // "Dec 13, 2026 12:00 AM"

@Column(name = "preferred_airport_arrival")
private String preferredAirportArrival;  // "Indira Gandhi Airport"

@Column(name = "preferred_station_arrival")
private String preferredStationArrival;  // "Central Station"
```

### TravelInfo Entity
```java
private String arrivalMode;
private String arrivalDateTime;
private String arrivalFlightNumber;
private String arrivalAirport;
private String departureMode;
private String departureDateTime;
private String departureFlightNumber;
private String departureAirport;
private Boolean needsPickup;
private Boolean needsDrop;
private String specialRequirements;
private String notes;
```

---

## Visual Layout

### RSVP Form - Expected Travel Schedule Section
```
┌─ Expected Travel Schedule ─────────────────┐
│                                            │
│  ⬇️ Arrival                    ⬆️ Departure │
│  Dec 11, 2026 12:00 AM         Dec 13, 2026 │
│  ✈️ Indira Gandhi Airport      12:00 AM    │
│                                🚂 Central   │
│                                   Station   │
└────────────────────────────────────────────┘
```

### Travel Info Form - Pre-populated Fields
```
Arrival Section:
├─ Mode of Transport: [Select]
├─ Arrival Date & Time: [2026-12-11T00:00]  ← Pre-filled
│  Default: Dec 11, 2026 12:00 AM
├─ Flight/Train Number: []
└─ Airport/Station: [Indira Gandhi Airport]  ← Pre-filled
   Default: Indira Gandhi Airport

Departure Section:
├─ Mode of Transport: [Select]
├─ Departure Date & Time: [2026-12-13T00:00]  ← Pre-filled
│  Default: Dec 13, 2026 12:00 AM
├─ Flight/Train Number: []
└─ Airport/Station: [Indira Gandhi Airport]  ← Pre-filled
   Default: Indira Gandhi Airport
```

---

## Use Cases

### Use Case 1: Guest with Event Defaults
```
Host adds guest → Guest logs in → Updates RSVP
├─ RSVP form shows:
│  ├─ Arrival: "Dec 11, 2026 12:00 AM" (event default)
│  ├─ Airport: "Indira Gandhi Airport" (event)
│  ├─ Departure: "Dec 13, 2026 12:00 AM" (event default)
│  └─ Station: "Central Station" (event)
├─ Guest accepts RSVP
└─ When adding travel info:
   ├─ Arrival date/time: Pre-filled with event default
   ├─ Arrival airport: Pre-filled with event default
   ├─ Can override if flight is different
   └─ Saves custom or default values
```

### Use Case 2: Guest with Custom Host Dates
```
Host adds guest with custom arrival: "Dec 10, 2026 6:00 PM"
Guest logs in → Updates RSVP
├─ RSVP form shows:
│  ├─ Arrival: "Dec 10, 2026 6:00 PM" (host set, overrides event)
│  ├─ Departure: "Dec 13, 2026 12:00 AM" (event default)
│  └─ Airports/Stations as usual
└─ Guest updates travel info:
   ├─ Arrival date/time: "Dec 10, 2026 6:00 PM" (guest preferred)
   ├─ Can modify if actual flight differs
   └─ Saves override if needed
```

### Use Case 3: Guest Modifying Travel Details
```
Guest already has travel info saved
When they click "Update Travel Details" again:
├─ Arrival date/time: [Their saved value - not guest preferred]
├─ Arrival airport: [Their saved value - not event default]
├─ Departure date/time: [Their saved value - not guest preferred]
└─ Departure airport: [Their saved value - not event default]
Note: Already saved values take precedence over defaults
```

---

## Files Modified

| File | Changes |
|------|---------|
| `GuestInvitationsController.java` | Added WeddingEventRepository, updated rsvpForm and travelInfoForm to fetch and pass event |
| `guest_rsvp_form.html` | Added Expected Travel Schedule alert section showing arrival/departure with airports/stations |
| `guest_travel_info_form.html` | Pre-populated arrival/departure date/time and airport fields with guest preferences or event defaults |

---

## Build Status

```
BUILD SUCCESS ✅
Total time: 15.294 s
```

---

## Future Enhancements

- [ ] Show flight/train recommendations based on airport selection
- [ ] Validate arrival/departure dates against wedding date
- [ ] Show conflict warnings if dates outside expected range
- [ ] Sync with calendar to show all guest travel schedules
- [ ] Email guest travel details confirmation
- [ ] Bulk export all guest travel schedules
- [ ] Time zone support for international guests

---

**Status:** ✅ Complete  
**Date:** January 7, 2026

