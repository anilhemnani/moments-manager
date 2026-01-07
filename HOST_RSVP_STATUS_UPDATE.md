# Host RSVP Status Update Feature

## Overview

Hosts can now update the RSVP status for invited guests directly from the UI. This allows hosts to mark responses for guests who need their status recorded without requiring guests to respond through the system.

---

## Features Implemented

### 1. **RSVP Status Update Endpoint** ✅
- **Endpoint:** `POST /guests/{guestId}/rsvp/update-status`
- **Access:** HOST role only
- **Parameters:**
  - `guestId` (path): ID of the guest
  - `status` (form): New RSVP status
  - `eventId` (form): ID of the event

### 2. **Status Options** ✅
Hosts can update guest RSVP status to:
- **Pending** - Response not yet received
- **Accepted** - Guest confirmed attendance
- **Declined** - Guest cannot attend
- **Maybe** - Guest is undecided

### 3. **UI Integration** ✅
RSVP view page now includes:
- **Status Update Form** - Only visible to hosts
- **Status Dropdown** - Pre-selected with current status
- **Update Button** - Submits status change
- **Success/Error Messages** - Feedback on status update

### 4. **Validation** ✅
- Only valid status values accepted (Pending, Accepted, Declined, Maybe)
- Guest must exist in system
- RSVP record must exist
- Only hosts can update status

---

## User Interface

### RSVP Status Card (With Host Update Option)

```
┌─────────────────────────────────────┐
│  RSVP Status                        │
├─────────────────────────────────────┤
│ Status: [Accepted Badge]            │
│ Attendee Count: 3                   │
│ Max Attendees: 5                    │
│                                     │
│ Update RSVP Status                  │
│ [Pending         ] [Update]         │
│    ↑ Current selection              │
└─────────────────────────────────────┘
```

### HTML Form

```html
<form action="/guests/{guestId}/rsvp/update-status" method="post">
    <select name="status" required>
        <option value="">-- Select Status --</option>
        <option value="Pending">Pending</option>
        <option value="Accepted">Accepted</option>
        <option value="Declined">Declined</option>
        <option value="Maybe">Maybe</option>
    </select>
    <input type="hidden" name="eventId" value="{eventId}"/>
    <button type="submit">Update</button>
</form>
```

---

## API Endpoint Details

### Update RSVP Status

```
POST /guests/{guestId}/rsvp/update-status
```

**Request Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| guestId | Path | Yes | Guest ID |
| status | Form | Yes | New RSVP status (Pending, Accepted, Declined, Maybe) |
| eventId | Form | Yes | Event ID |

**Response:**
- **Success:** Redirect to `/guests/{guestId}/rsvp` with success message
- **Error:** Redirect with error message displayed

**Example:**
```
POST /guests/1/rsvp/update-status
status=Accepted
eventId=1
```

---

## Code Implementation

### RSVPWebController.java

**New Methods Added:**

```java
/**
 * Host updates RSVP status for a guest
 */
@PreAuthorize("hasRole('HOST')")
@PostMapping("/update-status")
public String updateRSVPStatus(
        @PathVariable Long guestId,
        @RequestParam String status,
        @RequestParam Long eventId,
        Authentication authentication,
        RedirectAttributes redirectAttributes,
        Model model) {

    // Validation logic
    // Status update
    // Redirect with message
}

/**
 * Validate RSVP status values
 */
private boolean isValidRSVPStatus(String status) {
    return status != null && (
        status.equals("Pending") ||
        status.equals("Accepted") ||
        status.equals("Declined") ||
        status.equals("Maybe")
    );
}
```

---

## Workflow

### Host Updates Guest RSVP

```
1. Host navigates to guest RSVP view
   → GET /guests/{guestId}/rsvp

2. Host sees current RSVP status
   → Status displayed in badge

3. Host selects new status from dropdown
   → Options: Pending, Accepted, Declined, Maybe

4. Host clicks "Update" button
   → Form submitted: POST /guests/{guestId}/rsvp/update-status

5. Controller validates status
   ├─ Valid? → Update RSVP in database
   └─ Invalid? → Show error message

6. Redirect to RSVP view with message
   ├─ Success: "RSVP status updated from 'X' to 'Y'"
   └─ Error: Error message displayed
```

---

## Status Flow Diagram

```
         ┌─────────┐
         │ Pending │ ← Initial status
         └────┬────┘
              │
         ┌────┴────┬──────────┬──────────┐
         │          │          │          │
    ┌────▼──┐  ┌───▼──┐  ┌───▼──┐  ┌───▼──┐
    │Pending│  │Accept│  │Decline│  │Maybe │
    └───────┘  └──────┘  └───────┘  └──────┘
         │          │          │          │
         └────────────────────────────────┘
              ↓ (Any state to any state)
         Status Updated in DB
```

---

## Host Dashboard Integration

From Host Dashboard:
1. Click event → Manage
2. Click "Guests" card
3. Click guest name
4. Click "View RSVP"
5. Update status using dropdown

**URL Path:**
```
Host Dashboard
  → Event: Pratibha & Karthik Wedding
    → Guests: 150
      → Guest: Sharma Family (Ravi)
        → RSVP Status: Pending → [Update to Accepted]
```

---

## Error Handling

### Error Cases

| Error | Message | Action |
|-------|---------|--------|
| Guest not found | "Guest not found" | Redirect to dashboard |
| RSVP not found | "RSVP not found for this guest" | Redirect to RSVP view |
| Invalid status | "Invalid RSVP status" | Redirect with error |
| Unauthorized | 403 Forbidden | Spring Security blocks access |

### Success Message
```
"RSVP status updated from 'Pending' to 'Accepted'"
```

---

## Security Features

✅ **Role-Based Access:** Only ROLE_HOST can update status
✅ **Input Validation:** Status must be one of: Pending, Accepted, Declined, Maybe
✅ **Entity Validation:** Checks guest and RSVP exist
✅ **CSRF Protection:** Spring Security handles CSRF tokens
✅ **Feedback:** Clear success/error messages

---

## Testing Scenarios

### Scenario 1: Host Updates Pending to Accepted
1. Login as host
2. Navigate to guest RSVP
3. Current status: "Pending"
4. Select "Accepted" from dropdown
5. Click "Update"
6. **Expected:** Message "RSVP status updated from 'Pending' to 'Accepted'"

### Scenario 2: Host Updates Multiple Guests
1. Go through multiple guests
2. Update each to desired status
3. Each update shows success message
4. **Expected:** All updates reflected in database

### Scenario 3: Invalid Status (Edge Case)
1. Try to submit invalid status via browser console
2. **Expected:** Error message "Invalid RSVP status"

### Scenario 4: Non-Host Access
1. Login as guest
2. Try to access update endpoint
3. **Expected:** 403 Forbidden error

---

## Database

### RSVP Table Update

```sql
UPDATE rsvp_tbl 
SET status = 'Accepted' 
WHERE id = 1;
```

**Status Column:**
- Type: VARCHAR(50)
- Valid values: Pending, Accepted, Declined, Maybe
- Default: Pending

---

## UI Elements

### Status Badges (Display)
- **Accepted:** Green badge `bg-success`
- **Pending:** Yellow badge `bg-warning`
- **Declined:** Red badge `bg-danger`
- **Maybe:** Blue badge `bg-info`

### Form Elements
- Dropdown select with 4 options
- Hidden input for eventId
- Submit button with icon
- Only visible to hosts (sec:authorize="hasRole('HOST')")

---

## Future Enhancements

- [ ] Add timestamp for last status update
- [ ] Track who updated the status (host name)
- [ ] Add status change history/audit log
- [ ] Bulk update status for multiple guests
- [ ] Email notification when status changes
- [ ] Calendar view filtered by RSVP status
- [ ] Export RSVP report with status summary

---

**Status:** ✅ Complete and Ready  
**Build:** SUCCESS  
**Date:** January 6, 2026
s