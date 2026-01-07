# Subdomain Admin Update Feature

## Overview

Admins can now set and manage the subdomain for wedding events through the UI. The subdomain is **immutable after creation** - once set, it cannot be changed.

---

## Features Implemented

### 1. **Subdomain Field in Event Creation**
- **Location:** Event creation form (`/admin/events/new`)
- **Requirement:** Subdomain is mandatory (required field)
- **Format Rules:**
  - Must contain only lowercase letters, numbers, and hyphens
  - No spaces or special characters allowed
  - Pattern: `^[a-z0-9-]+$`
- **Uniqueness:** System checks that subdomain is not already taken
- **Example:** `pratibha-karthik`, `ravi-meera-wedding`, `john-doe-2025`

### 2. **Immutable Subdomain After Creation**
- **On Edit Form:** Subdomain field is disabled (read-only)
- **In Code:** Controller automatically preserves original subdomain
- **User Message:** Alert box explains subdomain cannot be changed
- **Location:** Both regular event form and admin event form show this warning

### 3. **Validation on Creation**
- ✅ Subdomain required validation
- ✅ Unique subdomain check (no duplicates)
- ✅ Format validation (lowercase, alphanumeric, hyphens only)
- ✅ User-friendly error messages
- ✅ Form re-displays with error if validation fails

### 4. **Success Messaging**
- Creation success message includes the public page URL
- Example: "Event created successfully! Public page: /public/pratibha-karthik"
- Update success message notes that subdomain cannot be changed

---

## User Interface Changes

### Event Creation Form
**Form Location:** `/events/new` and `/admin/events/new`

**Subdomain Field:**
```html
<input type="text" id="subdomain" 
       placeholder="e.g., pratibha-karthik"
       pattern="^[a-z0-9-]+$"
       required>
```

**Help Text:**
- Creation: "Unique identifier for public event page. Can only be set during creation."
- Editing: "⚠️ Immutable: Subdomain cannot be changed after event creation. Current: {subdomain}"

### Event Edit Form
**Field State:** Disabled (read-only)

**Message:** Shows warning with current subdomain value

---

## Controller Logic

### AdminEventController.java
**File:** `src/main/java/com/momentsmanager/web/AdminEventController.java`

**Endpoints:**
- `GET /admin/events` - List all events
- `GET /admin/events/new` - Show creation form
- `POST /admin/events/new` - Create event with validation
- `GET /admin/events/{id}/edit` - Show edit form
- `POST /admin/events/{id}/edit` - Update event (subdomain preserved)
- `POST /admin/events/{id}/delete` - Delete event

**Key Methods:**

```java
@PostMapping("/new")
public String createEvent(@ModelAttribute WeddingEvent event, ...) {
    // 1. Validate subdomain is provided
    // 2. Check subdomain is unique
    // 3. Validate format (alphanumeric + hyphens only)
    // 4. Save event
    // 5. Redirect with success message
}

@PostMapping("/{id}/edit")
public String updateEvent(@PathVariable Long id, @ModelAttribute WeddingEvent event, ...) {
    // 1. Preserve original subdomain
    // 2. Update all other fields
    // 3. Redirect with success message
}
```

### EventWebController.java
**File:** `src/main/java/com/momentsmanager/web/EventWebController.java`

Similar validation logic for host/admin event updates

---

## Validation Rules

### Subdomain Format
```
Pattern: ^[a-z0-9-]+$
- Lowercase letters (a-z)
- Numbers (0-9)
- Hyphens (-)

Invalid Examples:
- "Pratibha-Karthik" (uppercase)
- "pratibha karthik" (space)
- "pratibha_karthik" (underscore)
- "pratibha@karthik" (special character)
```

### Uniqueness Check
```java
if (weddingEventRepository.findBySubdomain(event.getSubdomain()).isPresent()) {
    // Error: Subdomain already taken
}
```

### Immutability Protection
```java
// On update, always preserve original subdomain
event.setSubdomain(existing.getSubdomain());
```

---

## Public Access

Once created, the event is accessible via:
```
http://localhost:8080/public/{subdomain}
```

Example:
```
http://localhost:8080/public/pratibha-karthik
```

---

## Error Messages

### Subdomain Required
```
"Subdomain is required"
```

### Already Taken
```
"Subdomain is already taken. Please choose a different one."
```

### Invalid Format
```
"Subdomain must contain only lowercase letters, numbers, and hyphens."
```

---

## Database

**Column:** `wedding_event_tbl.subdomain`
- Type: VARCHAR(50)
- Constraints: UNIQUE, NOT NULL
- Can only be set during INSERT
- Should never be updated via UPDATE

---

## Testing Checklist

- [ ] Create event with valid subdomain - success
- [ ] Create event without subdomain - error
- [ ] Create event with uppercase subdomain - error
- [ ] Create event with special characters - error
- [ ] Create event with duplicate subdomain - error
- [ ] Create event with subdomain that contains spaces - error
- [ ] Edit event - subdomain field is disabled
- [ ] Edit event - subdomain value is preserved
- [ ] Access public page with subdomain - works
- [ ] Success message shows public URL

---

## Files Modified/Created

| File | Type | Changes |
|------|------|---------|
| `AdminEventController.java` | ✅ Created | New admin events controller with validation |
| `EventWebController.java` | ✅ Updated | Added subdomain preservation in update |
| `event_form.html` | ✅ Updated | Added subdomain field |
| `admin_event_form.html` | ✅ Updated | Added subdomain field with immutability note |
| `WeddingEvent.java` | ✅ Existing | Has subdomain field with @Column(unique=true) |
| `WeddingEventRepository.java` | ✅ Existing | Has findBySubdomain() method |

---

## Security Considerations

✅ **Only admins can create/edit events** - @PreAuthorize("hasRole('ADMIN')")
✅ **Subdomain cannot be exploited** - Format validation prevents injection
✅ **Uniqueness enforced** - Database constraint + application check
✅ **Read-only on edit** - HTML disabled attribute + backend preservation
✅ **Hosts cannot modify subdomain** - Edit/delete buttons hidden from host UI
✅ **Hosts have view-only access** - Can only view event details, not modify

---

## Host Restrictions

### What Hosts Cannot Do
- ❌ Create new events
- ❌ Edit existing events (subdomain or any other field)
- ❌ Delete events
- ❌ Access edit form

### What Hosts Can Do
- ✅ View event list (only their own events)
- ✅ View event details
- ✅ Configure WhatsApp settings (separate endpoint)
- ✅ View messages and travel info

### UI Enforcement
The event list template hides edit/delete buttons for hosts:
```html
<!-- Edit and Delete buttons only for admins -->
<th:block th:if="${#authentication.getAuthorities().?[authority == 'ROLE_ADMIN'].size() > 0}">
  <a href="/events/{id}/edit">Edit</a>
  <button>Delete</button>
</th:block>
```

### Controller Enforcement
All event modification endpoints require ADMIN role:
```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/{id}/edit")
@PostMapping("/{id}/edit")
@PostMapping("/{id}/delete")
```

---

**Status:** ✅ Complete and Ready  
**Build:** SUCCESS  
**Date:** January 6, 2026

