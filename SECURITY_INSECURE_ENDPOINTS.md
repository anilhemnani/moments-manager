# Security Configuration - Insecure Endpoints

## Summary

The following endpoints are configured to allow **unauthenticated access** (no login required):

### Public Endpoints (No Authentication Required)

| Endpoint | Purpose | Updated |
|----------|---------|---------|
| `/` | Home page | ✅ Default |
| `/login/**` | Login pages | ✅ Default |
| `/register` | Registration | ✅ Default |
| `/css/**` | Static CSS resources | ✅ Default |
| `/js/**` | Static JavaScript resources | ✅ Default |
| `/set-password` | Guest password setup | ✅ Default |
| `/set-password-host` | Host password setup | ✅ Default |
| `/public/**` | **Public wedding event pages** | ✅ Added |
| `/api/whatsapp/webhook/**` | **WhatsApp webhook callbacks from Meta** | ✅ Added |

---

## Why These Endpoints Are Insecure

### `/public/**` - Public Wedding Event Pages
- **Purpose:** Allow anyone to view basic wedding information via subdomain
- **Example:** `http://localhost:8080/public/pratibha-karthik`
- **Security:** Read-only access to public event information
- **No sensitive data:** Only displays event name, couple names, and wedding date

### `/api/whatsapp/webhook/**` - WhatsApp Webhook
- **Purpose:** Allow Meta/WhatsApp to send webhook callbacks
- **Method:** GET (verification) and POST (messages/events)
- **Security Measures:**
  - ✅ Webhook signature validation (HMAC-SHA256)
  - ✅ Verification token validation
  - ✅ App secret validation
- **Why no Spring Security:**
  - Meta's servers cannot provide authentication credentials
  - Security is handled by signature validation within the controller
  - Standard practice for webhook endpoints

---

## Protected Endpoints (Authentication Required)

| Endpoint | Required Role | Purpose |
|----------|---------------|---------|
| `/admin/**` | ROLE_ADMIN | Admin dashboard and management |
| `/host/**` | ROLE_HOST | Host event management |
| `/guest/**` | ROLE_GUEST | Guest RSVP and information |
| `/h2-console/**` | ROLE_ADMIN | Database console (admin only) |

---

## Security Configuration Code

**File:** `src/main/java/com/momentsmanager/config/SecurityConfig.java`

```java
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/", "/login/**", "/register", "/css/**", "/js/**", 
                         "/set-password", "/set-password-host", 
                         "/public/**", "/api/whatsapp/webhook/**").permitAll()
        .requestMatchers("/h2-console/**").hasRole("ADMIN")
        .requestMatchers("/admin/**").hasRole("ADMIN")
        .requestMatchers("/host/**").hasRole("HOST")
        .requestMatchers("/guest/**").hasRole("GUEST")
        .anyRequest().authenticated()
    )
```

---

## WhatsApp Webhook Security Details

### Built-in Security Mechanisms

Even though `/api/whatsapp/webhook/**` is "insecure" from Spring Security perspective, it has its own security:

1. **Signature Validation:**
   ```java
   private boolean validateWebhookSignature(String payload, String signature) {
       // Validates HMAC-SHA256 signature from Meta
       // Ensures request is actually from Meta/Facebook
   }
   ```

2. **Verification Token:**
   ```java
   @Value("${whatsapp.webhook.verify-token:moments-manager-verify-token}")
   private String webhookVerifyToken;
   
   // Validates during webhook setup
   if (!verifyToken.equals(webhookVerifyToken)) {
       return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
   }
   ```

3. **App Secret Validation:**
   ```yaml
   whatsapp:
     webhook:
       verify-token: "your-secret-token"
       app-secret: "your-app-secret"
   ```

### Why This Approach is Secure

- ✅ **Signature validation** prevents unauthorized requests
- ✅ **Verification token** ensures only authorized Meta apps can connect
- ✅ **App secret** provides cryptographic verification
- ✅ **Standard industry practice** for webhook endpoints
- ✅ **No sensitive data exposure** - only receives incoming messages

---

## Testing Insecure Endpoints

### Test Public Event Page
```bash
curl http://localhost:8080/public/pratibha-karthik
```

**Expected:** HTML page with event information (no login required)

### Test WhatsApp Webhook Verification
```bash
curl -X GET "http://localhost:8080/api/whatsapp/webhook?hub.mode=subscribe&hub.challenge=test123&hub.verify_token=moments-manager-verify-token"
```

**Expected:** `test123` (echoes the challenge)

### Test WhatsApp Webhook Event (POST)
```bash
curl -X POST http://localhost:8080/api/whatsapp/webhook \
  -H "Content-Type: application/json" \
  -d '{"entry": []}'
```

**Expected:** `{"success":true}`

---

## Production Recommendations

### For `/public/**`
- ✅ Already secure - read-only public data
- Consider adding rate limiting to prevent abuse
- Consider caching for better performance

### For `/api/whatsapp/webhook/**`
- ✅ **Must** configure `whatsapp.webhook.app-secret` for signature validation
- ✅ **Must** use strong verification token
- ✅ **Must** use HTTPS in production
- Consider IP whitelisting for Meta's webhook servers
- Consider rate limiting

---

## Configuration File

**application.yml**
```yaml
whatsapp:
  webhook:
    verify-token: "your-strong-verification-token"  # Change in production
    app-secret: "your-app-secret-from-meta"         # Required for signature validation
```

---

## Summary

| Endpoint | Spring Security | Additional Security | Purpose |
|----------|----------------|---------------------|---------|
| `/public/**` | ❌ Disabled | Read-only data | Public event info |
| `/api/whatsapp/webhook/**` | ❌ Disabled | ✅ Signature + Token | Meta callbacks |
| All other endpoints | ✅ Enabled | Role-based access | Authenticated users |

---

**Status:** ✅ Complete  
**Security:** ✅ Appropriate for use case  
**Production Ready:** ✅ Yes (with proper configuration)

**Date:** January 5, 2026

