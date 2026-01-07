# Guest Self-Service Features - RSVP, Attendees, and Travel Details

## Overview

Guests can now fully manage their wedding attendance through the UI:
1. **Update RSVP Status** - Accept, Decline, or Maybe
2. **Add/Manage Attendees** - List family members attending
3. **Share Travel Details** - Flight, train, arrival/departure times, and pickup/drop needs

---

## Features Implemented

### 1. RSVP Management

**Endpoint:** `/invitations/rsvp/form` (GET)  
**Update:** `/invitations/rsvp/update` (POST)

**Guest can:**
- Select RSVP status: Pending, Accepted, Declined, Maybe
- Specify number of attendees (0 to max allowed)
- Update RSVP anytime

**Data Saved:**
- RSVP status
- Attendee count
- Updated timestamp

### 2. Attendees Management

**Endpoint:** `/invitations/attendees` (GET)

**Guest can:**
- Add family members/attendees
- Specify attendee name and type (Adult/Child)
- View all added attendees
- Delete attendees if needed
- Max attendees limit enforced

**Attendee Fields:**
- Name (required)
- Type: Adult or Child
- Age Group

### 3. Travel Information

**Endpoint:** `/invitations/travel-info` (GET)  
**Update:** `/invitations/travel-info/save` (POST)

**Guest can provide:**

**Arrival Details:**
- Transport mode (Flight, Train, Car, Bus, Other)
- Arrival date & time
- Flight number / Train number (if applicable)
- Airport / Station name

**Departure Details:**
- Transport mode
- Departure date & time
- Flight/Train number
- Airport/Station

**Assistance:**
- Needs pickup from airport/station
- Needs drop to airport/station

**Additional Info:**
- Special requirements (dietary, accessibility)
- Additional notes

---

## User Interface

### Guest Invitation View

Invitation card includes three action sections:

```
┌─ RSVP Status ──────────────────────┐
│ Current Status: [Badge]            │
│ Attendees: 3 / 5                   │
│ [Update RSVP Button]               │
└────────────────────────────────────┘

┌─ Attendees ────────────────────────┐
│ Manage who from your family         │
│ [Add/Manage Attendees Button]       │
└────────────────────────────────────┘

┌─ Travel Information ───────────────┐
│ Share your travel details           │
│ [Update Travel Details Button]      │
└────────────────────────────────────┘
```

### RSVP Form

```
Guest Information: [Read-only summary]

RSVP Status:
[Dropdown: Pending, Accepted, Declined, Maybe]

Number of Attendees:
[Input: 0 to max allowed]

[Update RSVP Button] [Cancel Button]
```

### Attendees Form

```
Your Information: [Card with status]

Attendees List:
┌─────────────────┬────────┬──────────┐
│ Name            │ Type   │ Actions  │
├─────────────────┼────────┼──────────┤
│ John Doe        │ Adult  │ [Delete] │
│ Jane Doe        │ Child  │ [Delete] │
└─────────────────┴────────┴──────────┘

[Add Attendee Modal]
```

### Travel Info Form

```
Arrival Section:
├─ Mode of Transport [Dropdown]
├─ Date & Time [DateTime Input]
├─ Flight/Train Number [Text]
└─ Airport/Station [Text]

Departure Section:
├─ Mode of Transport [Dropdown]
├─ Date & Time [DateTime Input]
├─ Flight/Train Number [Text]
└─ Airport/Station [Text]

Assistance Section:
├─ [ ] Need pickup?
└─ [ ] Need drop?

Additional Info:
├─ Special Requirements [TextArea]
└─ Additional Notes [TextArea]

[Save Travel Details Button] [Cancel Button]
```

---

## Controller Endpoints

### GuestInvitationsController

```java
GET  /invitations                  // List guest invitations
GET  /invitations/{id}             // View invitation
GET  /invitations/rsvp/form        // RSVP form
POST /invitations/rsvp/update      // Update RSVP
GET  /invitations/attendees        // Attendees list
GET  /invitations/travel-info      // Travel info form
POST /invitations/travel-info/save // Save travel info
```

---

## Data Model

### RSVP Entity

```
id: Long (PK)
guest: Guest (FK)
eventId: Long
status: String (Pending, Accepted, Declined, Maybe)
attendeeCount: int
attendees: List<Attendee>
```

### TravelInfo Entity

```
id: Long (PK)
guest: Guest (FK)
arrivalMode: String
arrivalDateTime: String
arrivalFlightNumber: String
arrivalAirport: String
departureMode: String
departureDateTime: String
departureFlightNumber: String
departureAirport: String
needsPickup: Boolean
needsDrop: Boolean
specialRequirements: String
notes: String
```

### Guest Entity (Updated)

```
...existing fields...
travelInfo: TravelInfo (One-to-One)
```

---

## Guest Workflow

```
1. Guest logs in with phone number
   ↓
2. Invitation view displayed automatically (if single)
   ├─ Shows wedding details
   └─ Shows 3 action cards

3. Guest clicks "Update RSVP"
   ├─ Select status
   ├─ Specify attendee count
   └─ Save

4. Guest clicks "Add/Manage Attendees"
   ├─ Add family members
   ├─ Specify adult/child
   └─ Save

5. Guest clicks "Update Travel Details"
   ├─ Enter flight/train info
   ├─ Select arrival/departure dates
   ├─ Specify assistance needs
   └─ Save

6. Guest sees confirmation messages
   └─ Redirected back to invitations
```

---

## Files Created/Modified

### New Files
- `GuestInvitationsController.java` - Guest endpoints (updated)
- `guest_rsvp_form.html` - RSVP form template
- `guest_attendees_form.html` - Attendees management template
- `guest_travel_info_form.html` - Travel info form template

### Modified Files
- `guest_invitation_view.html` - Added RSVP, attendees, travel sections

---

## Key Features

### Smart Defaults
- RSVP status defaults to "Pending"
- Attendee count defaults to 0
- Travel info fields optional

### Validation
- Attendee count limited to guest's max attendees
- Only authenticated guests can access
- Phone number verified for invitations

### Security
- `@PreAuthorize("hasRole('GUEST')")` on all endpoints
- Guests can only access their own invitations
- Data associated with authenticated guest's phone number

### UX Improvements
- Success messages after each update
- Clear error messages
- Intuitive Bootstrap forms
- Modal for adding attendees
- Read-only guest information display

---

## Integration Points

### Authentication
- Uses guest's family name + phone as principal
- Phone extracted from principal for lookups
- Session-based authentication

### Database
- One-to-One RSVP per Guest
- One-to-One TravelInfo per Guest
- Cascading saves and deletes

### Navigation
- Guest dashboard redirects to `/invitations`
- Single invitation auto-loads
- Multiple invitations show list view

---

## Future Enhancements

- [ ] Bulk attendee import from Excel
- [ ] Email confirmation of RSVP
- [ ] WhatsApp notifications for updates
- [ ] Attendee meal preferences
- [ ] Accommodation preferences
- [ ] Travel companions matching
- [ ] Real-time updates for hosts
- [ ] PDF invitation download

---

## Testing Checklist

### RSVP Form
- [ ] Load form with no existing RSVP
- [ ] Load form with existing RSVP
- [ ] Update status
- [ ] Update attendee count
- [ ] Verify attendee count limit enforced
- [ ] Verify redirect after update

### Attendees Form
- [ ] View list of attendees
- [ ] Add new attendee
- [ ] Delete attendee
- [ ] Show empty state when no attendees

### Travel Info Form
- [ ] Fill arrival details
- [ ] Fill departure details
- [ ] Check/uncheck pickup/drop
- [ ] Add special requirements
- [ ] Add notes
- [ ] Save and verify data

### Security
- [ ] Unauthenticated access blocked
- [ ] Guest can only see own invitations
- [ ] Guest can only modify own RSVP/travel info

---

**Status:** ✅ Complete  
**Build:** SUCCESS  
**Date:** January 7, 2026

