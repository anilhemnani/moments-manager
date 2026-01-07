# Guest Invitations Not Displaying - Root Cause & Fix

## Problem Summary

When guests log in to the system, they see no invitations even though invitations have been sent to them. The guest dashboard shows "No Invitations Yet" message.

## Root Cause Analysis

### Authentication Principal Format
When a guest logs in, the system creates an authentication principal in this format:
```
FamilyName_PhoneNumber

Example: "Sharma_+919876543210"
```

### InvitationLog Structure
The `InvitationLog` stores the phone number in the `whatsappNumber` field:
```
whatsappNumber: "+919876543210"  (phone only, no family name)
```

### The Bug
The original `GuestInvitationsController` was using `auth.getName()` directly without extracting the phone number:

```java
// WRONG - Uses full principal string "Sharma_+919876543210"
String guestPhoneNumber = auth.getName();

// Tries to find: InvitationLog where whatsappNumber = "Sharma_+919876543210"
// But whatsappNumber actually contains: "+919876543210"
// Result: NO MATCH → No invitations displayed
```

## Solution

### Extract Phone Number from Principal
The `extractPhoneNumber()` method properly extracts the phone number from the principal:

```java
private String extractPhoneNumber(String principal) {
    if (principal == null || !principal.contains("_")) {
        return principal;
    }
    // Split by underscore and get the last part (phone number)
    String[] parts = principal.split("_");
    return parts[parts.length - 1];
}
```

**Example:**
```
Input principal:  "Sharma_+919876543210"
                   ↓
Split by "_":    ["Sharma", "+919876543210"]
                   ↓
Get last part:    "+919876543210"
                   ↓
Database match:   whatsappNumber = "+919876543210" ✓ FOUND!
```

### Updated Flow

```
1. Guest logs in with Family Name + Mobile
   └─ Principal set to: "FamilyName_PhoneNumber"

2. Guest accesses /guest/dashboard
   └─ Redirects to /invitations

3. GuestInvitationsController.listInvitations():
   ├─ Gets auth principal: "Sharma_+919876543210"
   ├─ Extracts phone: "+919876543210"
   ├─ Queries: findByGuestPhoneNumber("+919876543210")
   ├─ Finds matching InvitationLog records
   └─ Returns invitations to guest

4. If single invitation → Redirect directly to it
5. If multiple invitations → Show invitation list
6. If no invitations → Show empty state
```

## Files Modified

### GuestInvitationsController.java

**Changes:**
1. Updated `listInvitations()` method to extract phone number
2. Updated `viewInvitation()` method to extract phone number
3. Added `extractPhoneNumber()` helper method

**Before:**
```java
String guestPhoneNumber = auth.getName();
// ❌ Wrong - contains "FamilyName_PhoneNumber"
```

**After:**
```java
String principal = auth.getName();
String guestPhoneNumber = extractPhoneNumber(principal);
// ✅ Correct - extracts just the phone number
```

## Verification

### Test Case: Guest Login & View Invitations

**Scenario:** Guest "Sharma" with phone "+919876543210" has received an invitation

1. **Login:**
   - Family Name: Sharma
   - Mobile: +919876543210
   - System creates principal: "Sharma_+919876543210"

2. **Access Invitations:**
   - Request: GET /guest/dashboard
   - Redirects to: GET /invitations
   - Controller extracts: "+919876543210"
   - Query: `findByGuestPhoneNumber("+919876543210")`
   - Database returns: InvitationLog(whatsappNumber="+919876543210")
   - Result: ✅ Invitations displayed

### Test Case: Single Invitation Auto-Redirect

1. Guest has exactly 1 invitation
2. Access GET /invitations
3. Controller extracts phone number correctly
4. Finds 1 invitation
5. Auto-redirects to GET /invitations/{invitationId}
6. Shows invitation view directly (no dashboard)

## Security Considerations

✅ Phone number extraction is safe (only uses underscore split)
✅ Invitations filtered by authenticated user's phone number
✅ Guests can only access invitations sent to their phone number
✅ No SQL injection risk (using JPA parameterized queries)

## Edge Cases Handled

| Case | Handling |
|------|----------|
| No underscore in principal | Returns principal as-is |
| NULL principal | Redirects to login |
| NULL auth object | Redirects to login |
| No invitations found | Shows empty state |
| Single invitation | Auto-redirects to invitation |
| Multiple invitations | Shows invitation list |
| Invalid invitationId | Throws RuntimeException |

## InvitationLog Data Example

```
InvitationLog {
  id: 1,
  invitation: Invitation{id: 101, title: "Wedding Invitation"},
  guest: Guest{id: 5, familyName: "Sharma"},
  whatsappNumber: "+919876543210",  ← Phone stored here
  deliveryStatus: "DELIVERED",
  sentAt: 2026-01-05T10:30:00,
  sentBy: "host1@example.com"
}
```

## Related Controllers & Classes

### Guest Authentication (AuthController)
```java
Authentication auth = new UsernamePasswordAuthenticationToken(
    guest.getFamilyName() + "_" + guest.getContactPhone(),  // Creates principal
    null,
    Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST"))
);
```

### Invitation Repository Methods
```java
List<InvitationLog> findByGuestPhoneNumber(String phoneNumber);
Optional<InvitationLog> findByInvitationIdAndGuestPhoneNumber(Long invitationId, String phoneNumber);
```

---

**Status:** ✅ Fixed  
**Date:** January 6, 2026

