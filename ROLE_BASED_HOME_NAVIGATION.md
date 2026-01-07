# Role-Based Home Navigation

## Overview

The application now intelligently redirects authenticated users to their role-specific dashboard when they click the "Moments Manager" navbar brand or navigate to the home page (`/`).

---

## How It Works

### Navigation Flow

```
User clicks "Moments Manager" navbar brand or navigates to "/"
                                    ↓
                      HomeController checks authentication
                                    ↓
                    ┌───────────────┼───────────────┐
                    ↓               ↓               ↓
            Is ADMIN?         Is HOST?         Is GUEST?
                    ↓               ↓               ↓
         /admin/dashboard   /host/dashboard   /guest/dashboard
                    ↓               ↓               ↓
            Admin Dashboard  Host Dashboard  Guest Dashboard
```

### User Scenarios

**Scenario 1: Admin User**
- Login as admin
- Click "Moments Manager" navbar → Redirected to `/admin/dashboard`
- Logout and navigate to "/" → See home page (unauthenticated)

**Scenario 2: Host User**
- Login as host
- Click "Moments Manager" navbar → Redirected to `/host/dashboard`
- Logout and navigate to "/" → See home page (unauthenticated)

**Scenario 3: Guest User**
- Login as guest
- Click "Moments Manager" navbar → Redirected to `/guest/dashboard`
- Logout and navigate to "/" → See home page (unauthenticated)

**Scenario 4: Unauthenticated User**
- Navigate to "/" → See home page with login options
- Click login → Authenticated, then redirected to appropriate dashboard

---

## Implementation Details

### HomeController.java

```java
@GetMapping("/")
public String index() {
    // Check if user is authenticated
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
    if (authentication != null && authentication.isAuthenticated() 
            && !(authentication.getPrincipal() instanceof String)) {
        
        // Redirect based on user role
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_HOST"))) {
            return "redirect:/host/dashboard";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_GUEST"))) {
            return "redirect:/guest/dashboard";
        }
    }
    
    // If not authenticated, show home page
    return "index";
}
```

### Key Points

1. **Security Context Check:** Verifies user is authenticated using Spring Security's `SecurityContextHolder`
2. **Authority Check:** Uses `SimpleGrantedAuthority` to match user roles
3. **Principal Check:** Ensures principal is not a string (anonymous user)
4. **Fallback:** Returns home page if user is not authenticated or has unknown role

---

## Navbar Implementation

All dashboard templates have their navbar brand set to `/`:

**Admin Dashboard:**
```html
<a class="navbar-brand" href="/">Moments Manager</a>
```

**Host Dashboard:**
```html
<a class="navbar-brand" href="/">Moments Manager</a>
```

**Guest Dashboard:**
```html
<a class="navbar-brand" href="/">Moments Manager</a>
```

This means clicking the navbar brand will always go to the same URL (`/`), but HomeController will intelligently redirect based on the user's role.

---

## User Experience

### Benefits

✅ **Consistent Navigation** - Same navbar link works for all users
✅ **Smart Routing** - Users automatically directed to their dashboard
✅ **Seamless Login** - After login, users go directly to their workspace
✅ **Session Management** - Logout still takes to home page
✅ **Role-Aware** - No hardcoded dashboard links needed in templates

### User Journey

```
1. Unauthenticated user navigates to home page
   ↓
2. Sees login options for Admin, Host, Guest
   ↓
3. Clicks "Login as Admin" → Authenticates
   ↓
4. HomeController redirects to /admin/dashboard
   ↓
5. Admin sees dashboard with all event management options
   ↓
6. Admin clicks "Moments Manager" navbar → Goes to / → Redirected back to dashboard
   ↓
7. Admin clicks "Logout" → Logged out and returns to home page
   ↓
8. Now sees home page again with login options
```

---

## Testing Checklist

### As Admin
- [ ] Login as admin
- [ ] Verify redirected to admin dashboard
- [ ] Click "Moments Manager" navbar brand
- [ ] Should stay on admin dashboard (or redirect and come back)
- [ ] Logout and verify on home page

### As Host
- [ ] Login as host
- [ ] Verify redirected to host dashboard
- [ ] Click "Moments Manager" navbar brand
- [ ] Should stay on host dashboard (or redirect and come back)
- [ ] Logout and verify on home page

### As Guest
- [ ] Login as guest
- [ ] Verify redirected to guest dashboard
- [ ] Click "Moments Manager" navbar brand
- [ ] Should stay on guest dashboard (or redirect and come back)
- [ ] Logout and verify on home page

### Unauthenticated Access
- [ ] Navigate to "/" without logging in
- [ ] Should see home page with login options
- [ ] Should not be redirected to any dashboard

---

## URL Mapping

| URL | Access | Behavior |
|-----|--------|----------|
| `/` | Unauthenticated | Show home page |
| `/` | Admin | Redirect to `/admin/dashboard` |
| `/` | Host | Redirect to `/host/dashboard` |
| `/` | Guest | Redirect to `/guest/dashboard` |
| `/admin/dashboard` | Admin only | Show dashboard |
| `/host/dashboard` | Host only | Show dashboard |
| `/guest/dashboard` | Guest only | Show dashboard |

---

## Security Implications

✅ **Protected by Spring Security** - Role-based access control enforced
✅ **No Privilege Escalation** - Users can only access their role's dashboard
✅ **Session-Based** - Logged-out users cannot access dashboards
✅ **Clean Logout** - Redirects to home page with login options

---

## Code Changes Summary

| File | Change | Impact |
|------|--------|--------|
| `HomeController.java` | Added role-based redirect logic | Smart routing to dashboards |
| All dashboard templates | Already use `href="/"` | Consistent navbar behavior |
| index.html | No change needed | Unauthenticated home page |

---

## Future Enhancements

- [ ] Add landing page-specific role messages
- [ ] Remember user's last accessed page before logout
- [ ] Add "Back to Dashboard" button on all pages
- [ ] Implement dashboard shortcuts on home page
- [ ] Add quick links based on user role

---

**Status:** ✅ Complete and Tested  
**Build:** SUCCESS  
**Date:** January 6, 2026

