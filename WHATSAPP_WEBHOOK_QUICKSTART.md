# WhatsApp Webhook Integration - Quick Start Guide

## 5-Minute Setup

### Step 1: Verify Database Migration
The application will automatically create the `guest_message_tbl` table on startup via Liquibase.

### Step 2: Configure application.yml
Update your `src/main/resources/application.yml`:

```yaml
whatsapp:
  webhook:
    verify-token: "your-secure-random-token"  # Change this!
    app-secret: ""                             # Optional: set if you have Meta app secret
    endpoint-path: "/api/whatsapp/webhook"
```

### Step 3: Configure Meta Webhook

1. Open [Meta App Dashboard](https://developers.facebook.com/apps/)
2. Navigate to your WhatsApp Business App
3. Go to **Webhooks** > **Configuration**
4. Add a new subscription for **messages** webhook
5. Fill in:
   - **Callback URL**: `https://your-domain.com/api/whatsapp/webhook`
   - **Verify Token**: (must match `whatsapp.webhook.verify-token` from Step 2)
6. Click **Verify and Save**

### Step 4: Subscribe to Webhook Fields
After verification, Meta will show webhook fields. Subscribe to:
- `messages` - To receive incoming messages
- `message_status` - To receive delivery/read status updates

### Step 5: Test the Integration

**Option A: Via Meta Webhook Test Tool**
1. Go to Webhooks configuration
2. Use "Send test message" to trigger a test webhook

**Option B: Check Logs**
Start the application and look for logs:
```
INFO c.m.w.WhatsAppWebhookController : Webhook verified successfully
DEBUG c.m.w.WhatsAppWebhookController : Processing webhook payload
```

## Access the Inbox

After a message is received from a guest:

1. Navigate to: `http://localhost:8080/inbox/events/{eventId}`
   - Replace `{eventId}` with your wedding event ID
2. You should see messages from guests
3. Click on a message to view details
4. Mark as read/unread

## Development Setup

### 1. Start Application
```bash
cd /home/anilhemnani/moments-manager
mvn spring-boot:run
```

### 2. Enable Debug Logging
Add to `application.yml`:
```yaml
logging:
  level:
    com.momentsmanager.service.MessageService: DEBUG
    com.momentsmanager.controller.WhatsAppWebhookController: DEBUG
```

### 3. Test Webhook Locally (ngrok)
```bash
# In another terminal, expose local server to internet
ngrok http 8080

# Use ngrok URL in Meta webhook configuration
# e.g., https://abc123.ngrok.io/api/whatsapp/webhook
```

### 4. View Database
```bash
# H2 Console at http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:weddingdb
# Query messages:
SELECT * FROM guest_message_tbl;
```

## Common Scenarios

### Scenario 1: Receive a Message from Guest
1. Guest sends WhatsApp to your business number
2. Meta sends webhook to `/api/whatsapp/webhook`
3. Application creates `GuestMessage` record
4. Host sees message in inbox with unread status

### Scenario 2: View Conversation with Guest
1. Host clicks on a guest name in inbox
2. Full conversation history loads
3. Messages automatically marked as read
4. Host can view message details

### Scenario 3: Send Invitation (Outbound)
1. Host sends invitation through invitation system
2. Message sent via `WhatsAppService.sendMessage()`
3. Meta webhook updates status to DELIVERED/READ
4. Status visible in message history

## File Structure

```
moments-manager/
├── src/main/java/com/momentsmanager/
│   ├── model/
│   │   └── GuestMessage.java                    # Message entity
│   ├── repository/
│   │   └── GuestMessageRepository.java          # Data access
│   ├── service/
│   │   └── MessageService.java                  # Business logic
│   ├── controller/
│   │   ├── WhatsAppWebhookController.java       # Webhook handler
│   │   └── dto/
│   │       └── WhatsAppWebhookPayload.java      # Meta payload DTOs
│   └── web/
│       └── HostInboxWebController.java          # UI endpoints
├── src/main/resources/
│   ├── application.yml                          # Configuration
│   ├── db/changelog/
│   │   └── db.changelog-master.xml              # Liquibase migration
│   └── templates/inbox/
│       ├── event_inbox.html                     # Inbox list
│       ├── conversation.html                    # Conversation view
│       └── message_detail.html                  # Message detail
└── WHATSAPP_WEBHOOK_INTEGRATION.md              # Full documentation
```

## API Quick Reference

### Webhook Endpoints
```
GET/POST /api/whatsapp/webhook - Meta webhook
```

### UI Endpoints
```
GET  /inbox/events/{eventId}                    - Inbox list
GET  /inbox/events/{eventId}/guests/{guestId}   - Conversation
GET  /inbox/messages/{messageId}                - Message detail
POST /inbox/messages/{messageId}/mark-read      - Mark read
POST /inbox/events/{eventId}/mark-all-read      - Mark all read
```

## Troubleshooting

### "Webhook token mismatch" error
- Verify `verify-token` in `application.yml` matches Meta configuration

### "No messages appearing in inbox"
- Check event ID is correct
- Verify guest phone number is in database
- Check logs for errors
- Query database:
  ```sql
  SELECT * FROM guest_message_tbl WHERE event_id = 1;
  ```

### "Webhook not being called"
- Verify callback URL is publicly accessible
- Check firewall/network settings
- Use ngrok for local development
- Test with curl from Meta's webhook test tool

## Next Steps

1. ✅ **Setup complete** - Webhook is configured and running
2. 📱 **Receive messages** - Guests can send messages
3. 📧 **View inbox** - Hosts can see and manage messages
4. 🔄 **Send replies** - (Future feature) Reply to guests

## Support Resources

- [Meta WhatsApp Cloud API Docs](https://developers.facebook.com/docs/whatsapp/cloud-api)
- [Full Integration Guide](./WHATSAPP_WEBHOOK_INTEGRATION.md)
- Application Logs: Check console output for errors
- Database: H2 console at `/h2-console`

---

**Status**: ✅ WhatsApp webhook integration is ready to use!

