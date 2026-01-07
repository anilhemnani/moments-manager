# Host Access Restrictions - Subdomain Protection

## Overview

Hosts are now **completely restricted from modifying events**, including the subdomain. This prevents hosts from accidentally or intentionally tampering with event settings.

---

## What Hosts Cannot Do

### ❌ Create Events
- No "New Event" button in UI
- No access to `/events/new` endpoint
- @PreAuthorize("hasRole('ADMIN')") blocks access

### ❌ Edit Events
- No "Edit" button in event list
- No access to `/events/{id}/edit` endpoint
- @PreAuthorize("hasRole('ADMIN')") blocks access
- **Cannot modify subdomain** (primary restriction)
- Cannot modify any event fields

### ❌ Delete Events
- No "Delete" button in event list
- No access to `/events/{id}/delete` endpoint
- @PreAuthorize("hasRole('ADMIN')") blocks access

---

## What Hosts CAN Do

### ✅ View Events
- See list of **their own events only**
- View complete event details
- View subdomain information

### ✅ Configure WhatsApp
- Separate endpoint for WhatsApp config
- Can update messaging settings
- Cannot modify core event data

### ✅ Access Dashboard Features
- View messages received from WhatsApp
- Manage guest travel information
- View RSVPs and invitations

---

## Technical Implementation

### Spring Security Annotations

All event modification endpoints have `@PreAuthorize("hasRole('ADMIN')"):

```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/new")
public String createEvent(...) { }

@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/{id}/edit")
public String editEvent(...) { }

@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/{id}/edit")
public String updateEvent(...) { }

@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/{id}/delete")
public String deleteEvent(...) { }
```

### UI Enforcement

Edit and Delete buttons are conditionally hidden for hosts in event_list.html:

```html
<!-- Edit and Delete buttons only for admins -->
<th:block th:if="${#authentication.getAuthorities().?[authority == 'ROLE_ADMIN'].size() > 0}">
  <a th:href="@{/events/{id}/edit}">Edit</a>
  <button onclick="deleteEvent()">Delete</button>
</th:block>
```

"Add New Event" button is also hidden for hosts:

```html
<th:block th:if="${#authentication.getAuthorities().?[authority == 'ROLE_ADMIN'].size() > 0}">
  <a href="/events/new" class="btn btn-success">Add New Event</a>
</th:block>
```

---

## Subdomain Protection Layers

1. **Database Layer**
   - UNIQUE constraint on subdomain column
   - NOT NULL constraint

2. **Application Layer**
   - Format validation: `^[a-z0-9-]+$`
   - Uniqueness check before insert
   - Immutability enforcement on update

3. **Access Control Layer**
   - Only ADMIN role can create/edit
   - Hosts cannot access edit endpoints
   - Spring Security @PreAuthorize annotations

4. **UI Layer**
   - Edit/Delete buttons hidden for hosts
   - Read-only subdomain display

---

## Host Access Matrix

| Feature | Admin | Host |
|---------|-------|------|
| Create Event | ✅ | ❌ |
| Edit Event | ✅ | ❌ |
| Delete Event | ✅ | ❌ |
| View Event List | ✅ | ✅ (own only) |
| View Event Details | ✅ | ✅ |
| View Subdomain | ✅ | ✅ |
| Modify Subdomain | ❌ (immutable) | ❌ |
| WhatsApp Config | ✅ | ✅ |
| View Messages | ✅ | ✅ |
| Manage Travel Info | ✅ | ✅ |

---

## Testing

### Test as Host
1. Login as host user
2. Navigate to `/events`
3. **Verify:**
   - ✅ Event list shows only host's events
   - ✅ No "Add New Event" button
   - ✅ No "Edit" button on events
   - ✅ No "Delete" button on events
   - ✅ Can click "View" to see event details
   - ✅ Subdomain is visible but read-only

### Test Access Control
1. Login as host
2. Try to manually access `/events/new` → Redirected
3. Try to manually access `/events/{id}/edit` → Access denied
4. Try to manually access `/events/{id}/delete` → Access denied

### Test as Admin
1. Login as admin user
2. Navigate to `/events`
3. **Verify:**
   - ✅ Event list shows all events
   - ✅ "Add New Event" button visible
   - ✅ "Edit" button on each event
   - ✅ "Delete" button on each event
   - ✅ Can create, edit, delete events
   - ✅ Can modify all event details (except cannot change existing subdomain)

---

## Files Modified

| File | Changes |
|------|---------|
| `EventWebController.java` | Added documentation, ensured @PreAuthorize on all edit endpoints |
| `event_list.html` | Hide Edit/Delete buttons for hosts, hide Add New Event button for hosts |
| `SUBDOMAIN_ADMIN_UPDATE.md` | Added host restriction information |

---

## Security Benefits

✅ **Prevents Accidental Changes** - Hosts cannot accidentally modify event settings
✅ **Prevents Subdomain Hijacking** - No host can change the subdomain
✅ **Enforces Admin Control** - Only admins can make critical changes
✅ **Defense in Depth** - Multiple layers: database, app, controller, UI
✅ **Audit Trail** - All changes must go through admin

---

**Status:** ✅ Complete and Implemented  
**Security Level:** HIGH  
**Date:** January 6, 2026

