# WhatsApp Webhook Callback URL Configuration

## Overview

The WhatsApp Cloud API webhook for the Moments Manager application uses a **single centralized endpoint** that handles callbacks for all wedding events. The webhook is event-agnostic at the endpoint level, but event context is determined through the WhatsApp business account and phone number ID configuration.

---

## Webhook Endpoint URL

### Base Webhook URL

```
POST https://your-domain.com/api/whatsapp/webhook
```

### Examples with Different Domains

**Local Development:**
```
http://localhost:8080/api/whatsapp/webhook
```

**Production Example:**
```
https://moments-manager.com/api/whatsapp/webhook
```

**Subdomain-Based (if applicable):**
```
https://pratibha-karthik.moments-manager.com/api/whatsapp/webhook
```

---

## Important: Single Endpoint for All Events

**Key Point:** The WhatsApp webhook endpoint is **NOT event-specific** in the URL path.

All wedding events share the **same webhook endpoint**: `/api/whatsapp/webhook`

Event context is determined by:
- **WhatsApp Business Phone Number ID** (configured per event in the database)
- **WhatsApp Business Account ID** (configured per event in the database)

The webhook payload includes the phone number ID, which the system uses to identify which event the message belongs to.

---

## Webhook Configuration in Meta/Facebook

### Step 1: Configure in Meta Developer Console

1. Go to **Meta App Dashboard** > Your App
2. Navigate to **WhatsApp** > **Configuration**
3. Set the **Callback URL** to:
   ```
   https://your-domain.com/api/whatsapp/webhook
   ```

### Step 2: Set Verification Token

In your application configuration (`application.yml`):
```yaml
whatsapp:
  webhook:
    verify-token: "your-secure-verification-token"
    app-secret: "your-app-secret"
```

### Step 3: Configure Event-Specific Details

For each wedding event in the database, configure:

```
whatsapp_phone_number_id: "YOUR_PHONE_NUMBER_ID"
whatsapp_business_account_id: "YOUR_BUSINESS_ACCOUNT_ID"
whatsapp_access_token: "YOUR_ACCESS_TOKEN"
whatsapp_api_version: "v24.0" (or latest)
whatsapp_verify_token: "your-secure-verification-token"
```

---

## Webhook Types and Events

The endpoint handles multiple event types:

### 1. **Message Events** (Incoming Messages)
- Guest sends a message via WhatsApp
- System records the message in `guest_message` table

### 2. **Status Updates**
- Message delivery confirmation
- Message read confirmation
- Message failed delivery

### 3. **Authentication Verification** (GET Request)
Meta sends a verification request during webhook setup:

```
GET /api/whatsapp/webhook?hub.mode=subscribe&hub.challenge=CHALLENGE_VALUE&hub.verify_token=TOKEN
```

The system responds with the `hub.challenge` value if the token matches.

---

## Request/Response Flow

### Meta → Application (GET - Verification)
```
GET /api/whatsapp/webhook?hub.mode=subscribe&hub.challenge=123456&hub.verify_token=your-token
```

**Response (200 OK):**
```
123456
```

### Meta → Application (POST - Events)
```
POST /api/whatsapp/webhook
Content-Type: application/json
X-Hub-Signature-256: sha256=...

{
  "entry": [{
    "changes": [{
      "value": {
        "messages": [{
          "from": "+91XXXXXXXXXX",
          "body": "message text",
          "id": "wamid.xxx"
        }],
        "metadata": {
          "phone_number_id": "YOUR_PHONE_NUMBER_ID",
          "display_phone_number": "+91XXXXXXXXXX"
        }
      }
    }]
  }]
}
```

**Response (200 OK):**
```json
{
  "success": true
}
```

---

## Security Features

### 1. **Webhook Signature Validation**
- Meta signs each webhook with X-Hub-Signature-256 header
- Application verifies signature using app secret
- Prevents unauthorized requests

### 2. **Verification Token**
- Meta requires a custom verification token during setup
- Must match `whatsapp.webhook.verify-token` in configuration

### 3. **HTTPS Required**
- Production must use HTTPS for webhook URL
- Local development can use HTTP for testing

---

## Database Storage

Event-specific WhatsApp configuration is stored in `wedding_event_tbl`:

| Column | Type | Purpose |
|--------|------|---------|
| `whatsapp_api_enabled` | BOOLEAN | Enable/disable WhatsApp integration |
| `whatsapp_phone_number_id` | VARCHAR | Meta-provided phone number ID |
| `whatsapp_business_account_id` | VARCHAR | Meta business account ID |
| `whatsapp_access_token` | VARCHAR | OAuth token for API calls |
| `whatsapp_api_version` | VARCHAR | API version (e.g., v24.0) |
| `whatsapp_verify_token` | VARCHAR | Verification token for webhook |

---

## How to Configure for Your Event

### Example: Pratibha & Karthik Wedding

1. **Create/Update the Event:**
   ```sql
   UPDATE wedding_event_tbl 
   SET whatsapp_api_enabled = true,
       whatsapp_phone_number_id = "123456789",
       whatsapp_business_account_id = "987654321",
       whatsapp_access_token = "EAAx...",
       whatsapp_api_version = "v24.0",
       whatsapp_verify_token = "pratibha-karthik-token"
   WHERE subdomain = 'pratibha-karthik';
   ```

2. **Configure Meta Webhook:**
   - URL: `https://your-domain.com/api/whatsapp/webhook`
   - Verify Token: `pratibha-karthik-token`
   - Subscribe to: messages, message_status

3. **Test Webhook:**
   ```bash
   curl -X GET "https://your-domain.com/api/whatsapp/webhook?hub.mode=subscribe&hub.challenge=test123&hub.verify_token=pratibha-karthik-token"
   ```

---

## Application Configuration

### application.yml
```yaml
whatsapp:
  api:
    url: "https://graph.facebook.com/v24.0/{PHONE_NUMBER_ID}/messages"
    token: "YOUR_ACCESS_TOKEN"  # This gets overridden per-event from database
  webhook:
    verify-token: "moments-manager-verify-token"
    app-secret: "YOUR_APP_SECRET"  # Optional but recommended
    endpoint-path: "/api/whatsapp/webhook"
```

---

## Testing the Webhook

### Using cURL

**Test Verification (GET):**
```bash
curl -X GET "http://localhost:8080/api/whatsapp/webhook?hub.mode=subscribe&hub.challenge=test123&hub.verify_token=moments-manager-verify-token"
```

**Test Message Event (POST):**
```bash
curl -X POST http://localhost:8080/api/whatsapp/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "entry": [{
      "changes": [{
        "value": {
          "messages": [{
            "from": "+919876543210",
            "body": "Hello!",
            "id": "wamid.test123"
          }],
          "metadata": {
            "phone_number_id": "123456789",
            "display_phone_number": "+919876543210"
          }
        }
      }]
    }]
  }'
```

---

## Webhook Controller Implementation

**File:** `src/main/java/com/momentsmanager/controller/WhatsAppWebhookController.java`

**Endpoints:**
- `GET /api/whatsapp/webhook` - Webhook verification
- `POST /api/whatsapp/webhook` - Receive events

**Features:**
- Signature validation
- Webhook payload parsing
- Message recording to database
- Status update handling
- Error handling and logging

---

## Important Notes

1. **Same Endpoint for All Events:** The `/api/whatsapp/webhook` endpoint handles all events
2. **Event Identification:** Events are identified by the `phone_number_id` in the webhook payload
3. **Database-Driven:** Event-specific tokens and IDs are stored in the database
4. **Security:** Always validate webhook signatures in production
5. **Retry Logic:** Meta retries webhook delivery if you don't respond with 200 OK
6. **Response Time:** Respond quickly (ideally within 5 seconds)

---

## Summary

| Aspect | Details |
|--------|---------|
| **Endpoint** | `/api/whatsapp/webhook` |
| **Method** | GET (verification) and POST (events) |
| **Authentication** | Verification token + signature validation |
| **Event Scope** | All events share this endpoint |
| **Event Context** | Determined by phone_number_id in payload |
| **Database** | Configuration stored per event in wedding_event_tbl |
| **Security** | HTTPS required, signature validation recommended |

---

**Status:** ✅ Ready for Integration with Meta WhatsApp API

**Date:** January 5, 2026


