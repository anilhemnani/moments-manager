# WhatsApp Webhook Integration - Testing Guide

## Quick Test

### 1. Verify Webhook Endpoint is Running
```bash
# Test the webhook endpoint
curl -I http://localhost:8080/api/whatsapp/webhook

# Expected: 405 Method Not Allowed (because GET without params)
```

### 2. Test Webhook Verification (Handshake)
```bash
curl "http://localhost:8080/api/whatsapp/webhook?hub.mode=subscribe&hub.challenge=test123&hub.verify_token=moments-manager-verify-token"

# Expected response: test123
```

### 3. Test Invalid Token
```bash
curl "http://localhost:8080/api/whatsapp/webhook?hub.mode=subscribe&hub.challenge=test123&hub.verify_token=invalid_token"

# Expected: 403 Forbidden
```

## Manual Testing with Sample Webhook Payload

### Test 1: Receive a Text Message

```bash
curl -X POST http://localhost:8080/api/whatsapp/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "object": "whatsapp_business_account",
    "entry": [
      {
        "id": "123456789",
        "changes": [
          {
            "field": "messages",
            "value": {
              "messaging_product": "whatsapp",
              "metadata": {
                "display_phone_number": "1234567890",
                "phone_number_id": "987654321"
              },
              "messages": [
                {
                  "id": "wamid.test.123",
                  "from": "919876543210",
                  "timestamp": "1640995200",
                  "type": "text",
                  "text": {
                    "body": "Hello, is this the right event?"
                  }
                }
              ]
            }
          }
        ]
      }
    ]
  }'
```

**Expected Result:**
- No error in response
- Message appears in database: `SELECT * FROM guest_message_tbl;`
- Message appears in inbox UI (if guest phone matches)

### Test 2: Receive an Image Message

```bash
curl -X POST http://localhost:8080/api/whatsapp/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "object": "whatsapp_business_account",
    "entry": [
      {
        "id": "123456789",
        "changes": [
          {
            "field": "messages",
            "value": {
              "messaging_product": "whatsapp",
              "metadata": {
                "display_phone_number": "1234567890",
                "phone_number_id": "987654321"
              },
              "messages": [
                {
                  "id": "wamid.test.image.456",
                  "from": "919876543210",
                  "timestamp": "1640995201",
                  "type": "image",
                  "image": {
                    "id": "image_id_123",
                    "mime_type": "image/jpeg",
                    "sha256": "abc123def456"
                  }
                }
              ]
            }
          }
        ]
      }
    ]
  }'
```

**Expected Result:**
- Image message stored with type "IMAGE"
- Media URL populated
- Message preview shows "[Image received]"

### Test 3: Message Status Update (Delivery)

```bash
curl -X POST http://localhost:8080/api/whatsapp/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "object": "whatsapp_business_account",
    "entry": [
      {
        "id": "123456789",
        "changes": [
          {
            "field": "message_status",
            "value": {
              "messaging_product": "whatsapp",
              "metadata": {
                "display_phone_number": "1234567890",
                "phone_number_id": "987654321"
              },
              "statuses": [
                {
                  "id": "wamid.test.123",
                  "status": "delivered",
                  "timestamp": "1640995200",
                  "recipient_id": "919876543210"
                }
              ]
            }
          }
        ]
      }
    ]
  }'
```

**Expected Result:**
- Message status updated to "DELIVERED"
- Database shows: `status = 'DELIVERED'`

### Test 4: Message Status Update (Read)

```bash
curl -X POST http://localhost:8080/api/whatsapp/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "object": "whatsapp_business_account",
    "entry": [
      {
        "id": "123456789",
        "changes": [
          {
            "field": "message_status",
            "value": {
              "messaging_product": "whatsapp",
              "metadata": {
                "display_phone_number": "1234567890",
                "phone_number_id": "987654321"
              },
              "statuses": [
                {
                  "id": "wamid.test.123",
                  "status": "read",
                  "timestamp": "1640995202",
                  "recipient_id": "919876543210"
                }
              ]
            }
          }
        ]
      }
    ]
  }'
```

**Expected Result:**
- Message status updated to "READ"
- `read_at` timestamp updated

## Testing with Database Queries

### View All Messages
```sql
SELECT * FROM guest_message_tbl;
```

### View Unread Messages
```sql
SELECT * FROM guest_message_tbl WHERE is_read = FALSE;
```

### View Messages by Guest Phone
```sql
SELECT * FROM guest_message_tbl WHERE guest_phone_number = '919876543210';
```

### View Messages by Status
```sql
SELECT * FROM guest_message_tbl WHERE status = 'DELIVERED';
```

### View Message Statistics
```sql
SELECT 
  COUNT(*) as total_messages,
  SUM(CASE WHEN is_read = FALSE THEN 1 ELSE 0 END) as unread_messages,
  SUM(CASE WHEN direction = 'INBOUND' THEN 1 ELSE 0 END) as inbound_messages,
  COUNT(DISTINCT guest_id) as unique_guests
FROM guest_message_tbl;
```

## Testing with H2 Console

1. Open browser to: `http://localhost:8080/h2-console`
2. JDBC URL: `jdbc:h2:mem:weddingdb`
3. Username: `sa`
4. Password: (leave blank)
5. Click Connect

Then run SQL queries to verify data.

## Testing UI Endpoints

### Access Inbox List (replace 1 with actual event ID)
```bash
curl http://localhost:8080/inbox/events/1
```

**Expected**: HTML page with inbox list

### Get Unread Count (AJAX)
```bash
curl http://localhost:8080/inbox/events/1/unread-count

# Expected: {"success": true, "unreadCount": 0}
```

### Get Statistics (AJAX)
```bash
curl http://localhost:8080/inbox/events/1/stats

# Expected: JSON with totalMessages, unreadMessages, etc.
```

### Mark Message as Read (AJAX)
```bash
curl -X POST http://localhost:8080/inbox/messages/1/mark-read \
  -H "Content-Type: application/json"

# Expected: {"success": true, "message": "Message marked as read"}
```

## Application Logs

### Enable Debug Logging

Add to `application.yml`:
```yaml
logging:
  level:
    com.momentsmanager.service.MessageService: DEBUG
    com.momentsmanager.controller.WhatsAppWebhookController: DEBUG
    com.momentsmanager.web.HostInboxWebController: DEBUG
```

### Check Logs for:
```
[DEBUG] Processing incoming message from: 919876543210
[INFO] Successfully fetched 1 templates from WhatsApp API
[ERROR] Error processing webhook event
[DEBUG] First template: {name=template_name, language=en_US}
```

## Integration Testing Steps

### Step 1: Setup
1. Ensure application is running on port 8080
2. Ensure H2 database is initialized with Liquibase migration
3. Create a test wedding event in database if needed

### Step 2: Simulate Guest Message
```bash
# Send test message payload
curl -X POST http://localhost:8080/api/whatsapp/webhook \
  -H "Content-Type: application/json" \
  -d '{"object":"whatsapp_business_account","entry":[{"id":"1","changes":[{"field":"messages","value":{"messaging_product":"whatsapp","metadata":{"display_phone_number":"1","phone_number_id":"1"},"messages":[{"id":"msg1","from":"919876543210","timestamp":"1640995200","type":"text","text":{"body":"Hello!"}}]}}]}]}'
```

### Step 3: Verify Message in Database
```bash
sqlite> SELECT * FROM guest_message_tbl WHERE guest_phone_number = '919876543210';
# Should show one record
```

### Step 4: Access Inbox UI
```bash
# Open browser to inbox for event 1
http://localhost:8080/inbox/events/1
```

### Step 5: Verify Message Appears
- Message should be visible in inbox list
- Should be marked as UNREAD
- Should show message type as TEXT
- Timestamp should be current or recent

### Step 6: Test Mark as Read
- Click on message in inbox
- Should redirect to message detail page
- Message should auto-mark as read
- Unread count should decrease

### Step 7: Test Conversation View
- From message detail, click "View Full Conversation"
- Should show all messages with this guest
- Messages should be in chronological order
- Earlier messages should already be marked as read

## End-to-End Testing Scenario

1. **Setup Phase**
   - Start application
   - Verify database is initialized
   - Create test event (id=1)
   - Create test guest with phone 919876543210

2. **Message Reception Phase**
   - Send text message via curl
   - Send image message via curl
   - Send status update via curl

3. **Verification Phase**
   - Check database for messages
   - Verify message content
   - Verify status updates
   - Check timestamps

4. **UI Testing Phase**
   - Access inbox list
   - Filter unread messages
   - View full conversation
   - Mark as read/unread
   - View message details

5. **Cleanup Phase**
   - Delete test messages
   - Delete test guest
   - Delete test event

## Common Test Issues

### Issue: "No messages in database"
**Solution:**
- Verify guest_message_tbl exists: `SELECT * FROM guest_message_tbl LIMIT 1;`
- Check application logs for errors
- Verify webhook endpoint is receiving POST requests
- Check JSON payload format

### Issue: "Message not matching guest"
**Solution:**
- Verify guest phone number in database matches webhook payload
- Use exact phone format with country code
- Check for leading/trailing spaces
- View unmatched messages by guest_id = NULL

### Issue: "Status updates not working"
**Solution:**
- Verify whatsapp_message_id matches in payload
- Check message status values are valid (SENT, DELIVERED, READ)
- Verify message exists before status update

### Issue: "UI shows no messages"
**Solution:**
- Verify event exists and event_id is correct
- Check that messages belong to correct event_id
- Query: `SELECT * FROM guest_message_tbl WHERE event_id = 1;`
- Check browser console for JavaScript errors

## Performance Testing

### Test with Large Message Count
```bash
# Generate 1000 test messages
for i in {1..1000}; do
  curl -X POST http://localhost:8080/api/whatsapp/webhook \
    -H "Content-Type: application/json" \
    -d "{\"object\":\"whatsapp_business_account\",\"entry\":[{\"id\":\"1\",\"changes\":[{\"field\":\"messages\",\"value\":{\"messaging_product\":\"whatsapp\",\"metadata\":{\"display_phone_number\":\"1\",\"phone_number_id\":\"1\"},\"messages\":[{\"id\":\"msg$i\",\"from\":\"919876543210\",\"timestamp\":\"1640995200\",\"type\":\"text\",\"text\":{\"body\":\"Test message $i\"}}]}}]}]}"
done

# Measure inbox page load time
curl -w "@curl-format.txt" -o /dev/null -s http://localhost:8080/inbox/events/1
```

## Success Criteria

- ✅ Webhook endpoint responds to verification requests
- ✅ Messages are stored in database with correct data
- ✅ Status updates are processed correctly
- ✅ Inbox UI displays messages correctly
- ✅ Read/unread status works
- ✅ Filtering works properly
- ✅ No errors in application logs
- ✅ Database queries execute within acceptable time
- ✅ UI pages load without errors
- ✅ Pagination works correctly

## Testing Checklist

- [ ] Webhook verification test passes
- [ ] Text message test passes
- [ ] Image message test passes
- [ ] Document message test passes
- [ ] Status update test passes
- [ ] Database queries return correct data
- [ ] H2 console shows messages
- [ ] Inbox UI loads without errors
- [ ] Message filtering works
- [ ] Mark as read/unread works
- [ ] Delete message works
- [ ] Statistics display correctly
- [ ] No errors in application logs
- [ ] Performance is acceptable
- [ ] All endpoints respond correctly

---

**Testing Status**: Ready for comprehensive testing
**Last Updated**: January 3, 2026

