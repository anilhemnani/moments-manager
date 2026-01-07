# Subdomain Feature Implementation

## Overview

The Wedding Event Subdomain feature allows each wedding event to have a unique, immutable subdomain identifier. This enables public access to a wedding event's information through a simple URL without requiring authentication.

**Key Characteristics:**
- ✅ Unique constraint: Each event must have a unique subdomain
- ✅ Immutable: Once created, the subdomain cannot be changed
- ✅ Admin-only creation: Only administrators can set the subdomain when creating an event
- ✅ No guest/host modification: Hosts and guests cannot update the subdomain
- ✅ Public access: Public pages can be accessed without login

---

## Database Schema Changes

### Wedding Event Table (`wedding_event_tbl`)

**New Column Added:**
```sql
subdomain VARCHAR(50) NOT NULL UNIQUE
```

**Liquibase Migration:**
- ChangeSet ID: `4-create-wedding-event-table` (updated)
- Creates the column with unique constraint at table creation time

---

## Entity Model Changes

### WeddingEvent Entity

**New Field:**
```java
@Column(name = "subdomain", unique = true, nullable = false, length = 50)
private String subdomain;
```

**New Methods:**
```java
public String getSubdomain() { return subdomain; }
public void setSubdomain(String subdomain) { this.subdomain = subdomain; }
```

**Constraints:**
- Cannot be null
- Must be unique across all events
- Maximum 50 characters
- Recommended format: lowercase alphanumeric with hyphens (e.g., `pratibha-karthik`)

---

## Repository Changes

### WeddingEventRepository

**New Method Added:**
```java
Optional<WeddingEvent> findBySubdomain(String subdomain);
```

This method enables querying events by their subdomain identifier.

---

## Public Controller - PublicEventController

**Location:** `src/main/java/com/momentsmanager/controller/PublicEventController.java`

**Endpoint:** `GET /public/{subdomain}`

**Features:**
- No authentication required
- Publicly accessible
- Returns event information if subdomain exists
- Returns 404 page if subdomain not found

**Response Data:**
- Event name
- Bride name
- Groom name
- Wedding date
- Event status

---

## Public Templates

### 1. Event Public Page

**File:** `src/main/resources/templates/public/event_public_page.html`

**Features:**
- Beautiful, responsive design
- Shows couple names prominently
- Displays wedding date
- Status badge (Published/Draft)
- Contact information section
- Mobile-friendly layout
- Gradient background with elegant styling

**URL Example:** `http://localhost:8080/public/pratibha-karthik`

---

### 2. Event Not Found Page

**File:** `src/main/resources/templates/public/event_not_found.html`

**Features:**
- Professional 404 error page
- Shows the requested subdomain
- Helpful message with instructions
- Contact information prompt
- Mobile-friendly layout

**URL Example:** `http://localhost:8080/public/invalid-subdomain`

---

## Seed Data

### Default Event

The seed data includes a sample wedding event with subdomain:

```xml
<insert tableName="wedding_event_tbl">
    <column name="id" valueNumeric="1"/>
    <column name="name" value="Pratibha &amp; Karthik Wedding"/>
    <column name="date" valueDate="2025-01-15"/>
    <column name="status" value="Published"/>
    <column name="bride_name" value="Pratibha"/>
    <column name="groom_name" value="Karthik"/>
    <column name="subdomain" value="pratibha-karthik"/>
</insert>
```

---

## Usage Guide

### For Administrators

#### Creating a New Event with Subdomain

1. Log in as admin
2. Create a new wedding event
3. Set the subdomain (required field)
4. Save the event
5. Subdomain cannot be changed later

**Subdomain Naming Convention:**
- Use lowercase letters only
- Use hyphens to separate words
- No spaces or special characters
- Maximum 50 characters
- Examples: `john-jane-wedding`, `smith-johnson-2025`

### For Public Access

#### Accessing Event Information

Simply visit the public URL:
```
http://localhost:8080/public/{subdomain}
```

Example:
```
http://localhost:8080/public/pratibha-karthik
```

---

## Security Considerations

### What is PUBLIC

- ✅ Event name
- ✅ Bride and groom names
- ✅ Wedding date
- ✅ Event status

### What is PROTECTED

- ❌ Guest list
- ❌ RSVP information
- ❌ Host details
- ❌ Financial data
- ❌ Travel information

**Access Control:**
- Public pages: No authentication required
- All other features: Authentication required
- Subdomain modification: Admin only
- Subdomain creation: Admin only

---

## Database Constraints

```sql
CONSTRAINT pk_wedding_event PRIMARY KEY (id)
CONSTRAINT uq_wedding_event_subdomain UNIQUE (subdomain)
CONSTRAINT nn_wedding_event_subdomain NOT NULL (subdomain)
```

---

## API/Repository Methods

### Find by Subdomain
```java
Optional<WeddingEvent> findBySubdomain(String subdomain);
```

**Usage:**
```java
Optional<WeddingEvent> event = weddingEventRepository.findBySubdomain("pratibha-karthik");
if (event.isPresent()) {
    // Event found
    WeddingEvent weddingEvent = event.get();
}
```

---

## Testing the Feature

### Test URLs

1. **Valid Subdomain:**
   ```
   http://localhost:8080/public/pratibha-karthik
   ```
   Expected: Shows the wedding event information

2. **Invalid Subdomain:**
   ```
   http://localhost:8080/public/non-existent-event
   ```
   Expected: Shows 404 "Event Not Found" page

---

## Future Enhancement Possibilities

- [ ] Custom subdomain validation rules
- [ ] Subdomain availability checker
- [ ] Bulk event creation with subdomains
- [ ] Subdomain analytics and visit tracking
- [ ] SEO optimization for public pages
- [ ] Shareable event links with custom messages
- [ ] Event countdown timer on public page
- [ ] Image gallery for events
- [ ] Guest list (public segment) on public page

---

## Implementation Summary

| Component | Status | Location |
|-----------|--------|----------|
| Database Column | ✅ | wedding_event_tbl.subdomain |
| Entity Field | ✅ | WeddingEvent.subdomain |
| Repository Method | ✅ | WeddingEventRepository.findBySubdomain() |
| Public Controller | ✅ | PublicEventController.java |
| Public Template | ✅ | public/event_public_page.html |
| 404 Template | ✅ | public/event_not_found.html |
| Liquibase Migration | ✅ | db.changelog-master.xml |
| Seed Data | ✅ | db.changelog-master.xml |
| Unit Tests | ⏳ | Pending |

---

**Status:** ✅ Complete and Ready for Use

**Build:** SUCCESS  
**Date:** January 5, 2026

e f