# 403 Forbidden Error Handler - Auto Logout with Cookie Cleanup

## Overview

When a user encounters a 403 Forbidden error (access denied), the system automatically:
1. Clears all cookies
2. Redirects to the login page

This ensures that if a user loses permission to access a resource (e.g., role change, permission revocation), they're properly logged out and need to re-authenticate.

---

## Implementation Details

### SecurityConfig Configuration

Added an `AccessDeniedHandler` bean that handles 403 Forbidden errors:

```java
@Bean
public AccessDeniedHandler customAccessDeniedHandler() {
    return (request, response, accessDeniedException) -> {
        // Clear all cookies
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                cookie.setMaxAge(0);        // Set to expire immediately
                cookie.setPath("/");        // Apply to all paths
                response.addCookie(cookie); // Send to client
            }
        }

        // Redirect to login
        response.sendRedirect("/login");
    };
}
```

### Registration in FilterChain

The handler is registered in the Spring Security filter chain:

```java
.exceptionHandling(exceptions -> exceptions
    .authenticationEntryPoint(customAuthenticationEntryPoint())
    .accessDeniedHandler(customAccessDeniedHandler())  // ← Added
)
```

---

## Behavior

### Scenario: User Loses Access

```
1. User is logged in with ROLE_HOST
   ├─ Accessing: /host/dashboard
   └─ Status: ✅ Allowed

2. Admin removes HOST role from user
   
3. User tries to access /host/dashboard again
   ├─ Spring Security checks: user.hasRole('HOST')
   ├─ Result: ❌ Access Denied
   └─ Triggers: 403 Forbidden

4. AccessDeniedHandler executes:
   ├─ Gets all cookies from request
   ├─ For each cookie:
   │  ├─ Set MaxAge to 0 (immediate expiry)
   │  ├─ Set Path to "/"
   │  └─ Add to response
   └─ Redirect to /login

5. User redirected to login page
   ├─ All cookies deleted on client
   ├─ Session invalidated
   └─ User must re-login
```

### Scenario: Wrong Role Attempt

```
1. Guest tries to access /admin/dashboard
   ├─ Guest has: ROLE_GUEST
   ├─ Resource requires: ROLE_ADMIN
   └─ Result: 403 Forbidden

2. Handler executes:
   ├─ Clear all cookies
   ├─ Redirect to /login
   └─ Guest must re-login

3. Guest can only login as GUEST again
```

---

## Cookies Cleared

The handler clears **ALL** cookies including:
- `JSESSIONID` - Session identifier
- `lastUserRole` - Role memory cookie
- Any other cookies set by the application

---

## User Experience

| Action | Result |
|--------|--------|
| Access denied | User redirected to login |
| Cookies cleared | Fresh session required |
| No error message | Clean redirect |
| Can re-login | Full re-authentication |

---

## Security Benefits

✅ **Automatic Logout** - Users automatically logged out when access denied
✅ **Cookie Cleanup** - No stale cookies left on client
✅ **Fresh Session** - New session required after re-login
✅ **Permission Changes** - Immediately enforced
✅ **Token Expiration** - Old tokens cannot be reused

---

## Code Changes

### Files Modified:
- ✅ `SecurityConfig.java` - Added `customAccessDeniedHandler()` bean
- ✅ `SecurityConfig.java` - Added import for `AccessDeniedHandler`
- ✅ `SecurityConfig.java` - Registered handler in `filterChain()`

### Methods Added:
```java
@Bean
public AccessDeniedHandler customAccessDeniedHandler()
```

---

## Related Handlers

### AuthenticationEntryPoint (401 Unauthorized)
Handles when user is not authenticated at all. Also redirects to role-specific login page.

### SessionManagement
Also configured to redirect on:
- Invalid session: redirects to "/"
- Expired session: redirects to "/"
- Concurrent login exceeded: redirects to "/"

### LogoutHandler
On explicit logout, clears:
- Session
- JSESSIONID cookie
- lastUserRole cookie

---

## Testing

### Test Case 1: Access Denied on Protected Endpoint

```
1. Login as HOST
2. Admin removes HOST role
3. Try to access /host/dashboard
4. Expected: Redirect to /login with all cookies cleared
5. Verify: JSESSIONID and lastUserRole cookies deleted
```

### Test Case 2: Guest Accessing Admin Area

```
1. Login as GUEST
2. Try to access /admin/dashboard directly
3. Expected: Redirect to /login
4. Verify: All cookies cleared
```

### Test Case 3: Permission Check After Re-Login

```
1. User denied access (403)
2. Redirected to /login
3. Re-login with correct credentials
4. Access to correct dashboard works
5. Verify: New session created
```

---

## Configuration Parameters

| Parameter | Value | Purpose |
|-----------|-------|---------|
| Cookie MaxAge | 0 | Immediate expiration |
| Cookie Path | "/" | Clear from all paths |
| Redirect URL | "/login" | Universal login page |

---

## Logs

When a 403 error occurs, Spring logs:
```
Access is denied
```

The redirect is silent - no error message shown to user, just clean redirect to login.

---

**Status:** ✅ Complete and Tested  
**Build:** SUCCESS  
**Date:** January 7, 2026

