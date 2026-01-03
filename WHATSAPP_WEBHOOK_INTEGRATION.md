# WhatsApp Cloud API Webhook Integration - Implementation Guide

## Overview

This document describes the WhatsApp Cloud API webhook integration feature that enables the Moments Manager application to:

1. **Receive incoming messages** from guests via WhatsApp
2. **Track message status** (sent, delivered, read, failed)
3. **Store messages** in the database with read/unread status
4. **Display inbox** for hosts to view and manage conversations with guests

## Architecture

### Components Created

#### 1. **Database Entity: GuestMessage** (`src/main/java/com/momentsmanager/model/GuestMessage.java`)

Stores WhatsApp messages with the following fields:

- `id`: Primary key (auto-generated)
- `event`: Reference to the wedding event
- `guest`: Reference to the guest (if phone number can be matched)
- `guestPhoneNumber`: Phone number of the message sender
- `messageContent`: Text content of the message
- `direction`: INBOUND (from guest) or OUTBOUND (to guest)
- `messageType`: TEXT, IMAGE, DOCUMENT, AUDIO, VIDEO, LOCATION, CONTACT
- `mediaUrl`: URL if message contains media
- `isRead`: Read/unread status (for hosts viewing messages)
- `whatsappMessageId`: Message ID from Meta API
- `status`: PENDING, SENT, DELIVERED, READ, FAILED
- `createdAt`: Timestamp when message was created
- `readAt`: Timestamp when host marked as read

**Database Table**: `guest_message_tbl`

**Indexes**:
- `idx_event_is_read` - for filtering unread messages
- `idx_guest_event` - for conversation retrieval
- `idx_timestamp` - for chronological sorting
- `idx_whatsapp_message_id` - for status updates

#### 2. **Repository: GuestMessageRepository** (`src/main/java/com/momentsmanager/repository/GuestMessageRepository.java`)

Custom Spring Data JPA repository with queries for:

- Finding all messages for an event
- Finding unread messages
- Finding messages by guest
- Finding messages by phone number
- Finding inbound/outbound messages
- Counting unread messages
- Finding messages by status

#### 3. **Service: MessageService** (`src/main/java/com/momentsmanager/service/MessageService.java`)

Business logic layer providing:

- Retrieving messages (paginated or filtered)
- Marking messages as read/unread
- Storing incoming messages
- Updating message status from webhooks
- Grouping messages by guest
- Getting conversation history
- Message statistics

#### 4. **REST Controller: WhatsAppWebhookController** (`src/main/java/com/momentsmanager/controller/WhatsAppWebhookController.java`)

Handles webhook callbacks from Meta:

**GET `/api/whatsapp/webhook`** - Webhook verification
- Called by Meta when setting up the webhook
- Validates the verify token
- Returns the challenge value

**POST `/api/whatsapp/webhook`** - Webhook events
- Receives incoming messages from guests
- Receives status updates (delivery, read receipts)
- Validates webhook signature (HMAC-SHA256)
- Processes and stores messages in database

#### 5. **Web Controller: HostInboxWebController** (`src/main/java/com/momentsmanager/web/HostInboxWebController.java`)

Provides web UI endpoints for hosts:

- `GET /inbox/events/{eventId}` - Display inbox for event
- `GET /inbox/events/{eventId}/guests/{guestId}` - View conversation with guest
- `GET /inbox/messages/{messageId}` - View message detail
- `POST /inbox/messages/{messageId}/mark-read` - Mark as read
- `POST /inbox/messages/{messageId}/mark-unread` - Mark as unread
- `POST /inbox/events/{eventId}/mark-all-read` - Mark all as read
- `GET /inbox/events/{eventId}/unread-count` - Get unread count
- `GET /inbox/events/{eventId}/stats` - Get statistics

#### 6. **DTOs: WhatsAppWebhookPayload** (`src/main/java/com/momentsmanager/controller/dto/WhatsAppWebhookPayload.java`)

Maps Meta's JSON webhook payload with support for:

- Text messages
- Media messages (image, document, audio, video)
- Location messages
- Contact messages
- Message status updates
- Error notifications

#### 7. **Thymeleaf Templates**

- `templates/inbox/event_inbox.html` - Inbox list view with filters and statistics
- `templates/inbox/conversation.html` - Conversation view with full message history
- `templates/inbox/message_detail.html` - Single message detail with context

## Configuration

### application.yml Settings

```yaml
whatsapp:
  webhook:
    verify-token: "moments-manager-verify-token"  # Change in production
    app-secret: ""                                  # Set to your app secret for signature validation
    endpoint-path: "/api/whatsapp/webhook"         # Webhook endpoint path
```

### Environment Variables (Optional)

For production, use environment variables:

```bash
WHATSAPP_WEBHOOK_VERIFY_TOKEN=your-secure-token
WHATSAPP_WEBHOOK_APP_SECRET=your-app-secret
```

## Meta/Facebook Configuration

### Setting Up the Webhook

1. Go to [Meta App Dashboard](https://developers.facebook.com/apps/)
2. Select your WhatsApp Business application
3. Go to **Webhooks** configuration
4. Add new subscription for **messages**
5. Set **Callback URL** to: `https://your-domain/api/whatsapp/webhook`
6. Set **Verify Token** to match your `whatsapp.webhook.verify-token` in application.yml
7. Click **Verify and Save**

### Webhook Verification

When Meta submits the callback URL, it will send a GET request with:
- `hub.mode=subscribe`
- `hub.challenge=<random_string>`
- `hub.verify_token=<your_verify_token>`

The webhook controller validates the token and returns the challenge.

### Security: Webhook Signature Validation

For production security, Meta sends an `X-Hub-Signature-256` header with each webhook event.

**To enable signature validation:**

1. Copy your **App Secret** from Meta App Dashboard
2. Set in application.yml:
   ```yaml
   whatsapp:
     webhook:
       app-secret: "your-app-secret-from-meta"
   ```

3. The controller will:
   - Extract the signature from `X-Hub-Signature-256` header
   - Calculate HMAC-SHA256 hash of the request body using your app secret
   - Compare with the signature from Meta
   - Reject if signatures don't match (403 Forbidden)

## Webhook Payload Structure

### Incoming Message Example

```json
{
  "object": "whatsapp_business_account",
  "entry": [
    {
      "id": "PHONE_NUMBER_ID",
      "changes": [
        {
          "field": "messages",
          "value": {
            "messaging_product": "whatsapp",
            "metadata": {
              "phone_number_id": "PHONE_NUMBER_ID",
              "display_phone_number": "1234567890"
            },
            "messages": [
              {
                "id": "wamid.XXXXX",
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
}
```

### Message Status Update Example

```json
{
  "object": "whatsapp_business_account",
  "entry": [
    {
      "id": "PHONE_NUMBER_ID",
      "changes": [
        {
          "field": "message_status",
          "value": {
            "messaging_product": "whatsapp",
            "metadata": {
              "phone_number_id": "PHONE_NUMBER_ID"
            },
            "statuses": [
              {
                "id": "wamid.XXXXX",
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
}
```

## Message Flow

### Receiving a Message

1. Guest sends WhatsApp message to your business number
2. Meta sends webhook POST to `/api/whatsapp/webhook`
3. Controller validates webhook signature
4. Extracts message details (sender, content, timestamp, type)
5. Matches sender phone to existing guest in database
6. Creates `GuestMessage` record with:
   - Direction: INBOUND
   - Status: DELIVERED
   - Is Read: false
7. Host sees message in inbox UI

### Sending a Message

1. Host sends message through invitation system
2. `WhatsAppService.sendMessage()` sends via Meta Cloud API
3. Message is logged (future enhancement: log as GuestMessage with OUTBOUND direction)
4. Meta webhook sends status updates to `/api/whatsapp/webhook`
5. Status updates (SENT, DELIVERED, READ) are processed
6. `GuestMessage` record status is updated

## Using the Inbox UI

### Access Inbox

Hosts can access inbox at: `/inbox/events/{eventId}`

### Features

1. **Message List View**
   - All messages from guests for selected event
   - Grouped by guest
   - Shows read/unread status
   - Last message preview
   - Timestamp
   - Message type badge (TEXT, IMAGE, etc.)

2. **Filters**
   - All Messages
   - Unread Only
   - Inbound Only (from guests)

3. **Statistics**
   - Total messages count
   - Unread messages count
   - Messages from guests count
   - Unique guests count

4. **Actions**
   - Mark individual message as read/unread
   - Mark all messages as read
   - View full conversation with guest
   - View message details
   - Delete message

5. **Conversation View**
   - Full message history with guest
   - Messages sorted chronologically
   - Status indicators (SENT, DELIVERED, READ)
   - Auto-marks messages as read when viewing
   - Media attachments display

### Managing Unread Messages

- Inbox shows unread count badge
- Filter to show only unread messages
- Mark as read individually or in bulk
- Read status persists in database

## Database Schema

```sql
CREATE TABLE guest_message_tbl (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    guest_id BIGINT,
    guest_phone_number VARCHAR(50) NOT NULL,
    message_content TEXT,
    direction VARCHAR(20) NOT NULL,
    message_type VARCHAR(50) DEFAULT 'TEXT',
    media_url TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    whatsapp_message_id VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    read_at TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES wedding_event_tbl(id),
    FOREIGN KEY (guest_id) REFERENCES guest_tbl(id),
    INDEX idx_event_is_read (event_id, is_read),
    INDEX idx_guest_event (guest_id, event_id),
    INDEX idx_timestamp (created_at),
    INDEX idx_whatsapp_message_id (whatsapp_message_id)
);
```

## API Endpoints Reference

### Webhook Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/whatsapp/webhook` | Webhook verification from Meta |
| POST | `/api/whatsapp/webhook` | Receive messages and status updates |

### Web UI Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/inbox/events/{eventId}` | View event inbox |
| GET | `/inbox/events/{eventId}/guests/{guestId}` | View conversation |
| GET | `/inbox/messages/{messageId}` | View message detail |
| POST | `/inbox/messages/{messageId}/mark-read` | Mark as read |
| POST | `/inbox/messages/{messageId}/mark-unread` | Mark as unread |
| POST | `/inbox/events/{eventId}/mark-all-read` | Mark all as read |
| GET | `/inbox/events/{eventId}/unread-count` | Get unread count |
| GET | `/inbox/events/{eventId}/stats` | Get statistics |

## Future Enhancements

1. **Reply Functionality**
   - Allow hosts to reply to guest messages directly from inbox
   - Send outbound messages and log them

2. **Media Handling**
   - Download media from Media URLs
   - Display images/documents in conversation
   - Support media uploads

3. **Search & Advanced Filtering**
   - Search messages by content
   - Filter by date range
   - Filter by message type

4. **Notifications**
   - Email/SMS notifications for new messages
   - Browser notifications
   - Unread count in navigation

5. **Bulk Operations**
   - Bulk mark as read/unread
   - Bulk delete
   - Bulk export

6. **Message Analytics**
   - Response time tracking
   - Guest engagement metrics
   - Message frequency trends

7. **Auto-Assignment**
   - Auto-assign unmatched phone numbers to guests
   - Suggest guest matches for manual confirmation

## Troubleshooting

### Webhook Not Receiving Messages

1. Check webhook endpoint is publicly accessible
2. Verify webhook verify-token matches Meta configuration
3. Check application logs for errors
4. Test with curl:
   ```bash
   curl -X POST http://localhost:8080/api/whatsapp/webhook \
     -H "Content-Type: application/json" \
     -d '{"test": "message"}'
   ```

### Messages Not Appearing in Inbox

1. Check `guest_message_tbl` table in database
2. Verify event ID in webhook payload
3. Check guest phone number matching logic
4. Enable debug logging:
   ```yaml
   logging:
     level:
       com.momentsmanager.service.MessageService: DEBUG
       com.momentsmanager.controller.WhatsAppWebhookController: DEBUG
   ```

### Signature Validation Failing

1. Verify `app-secret` is correct from Meta Dashboard
2. Ensure webhook events have `X-Hub-Signature-256` header
3. Check logs for signature mismatch errors

## Security Considerations

1. **Webhook Signature Validation** - Always validate signatures in production
2. **HTTPS Only** - Use HTTPS for webhook endpoint in production
3. **Token Security** - Use strong, random verify token
4. **Data Privacy** - Messages are stored with PII (phone numbers, content)
5. **Access Control** - Inbox access should be restricted to event hosts
6. **Rate Limiting** - Consider adding rate limiting for webhook endpoint

## Liquibase Migration

Migration ID: `24-create-guest-message-table`

Automatically creates the `guest_message_tbl` table with all required columns and indexes when application starts (Liquibase handles this).

To rollback:

```xml
<changeSet id="24-rollback" author="auto">
    <dropTable tableName="guest_message_tbl"/>
</changeSet>
```

## Example Usage

### Retrieve Event Inbox
```java
Page<GuestMessage> messages = messageService.getEventMessages(1L, PageRequest.of(0, 20));
```

### Mark Message as Read
```java
messageService.markAsRead(123L);
```

### Get Conversation with Guest
```java
List<GuestMessage> conversation = messageService.getConversation(1L, 5L);
```

### Get Statistics
```java
Map<String, Object> stats = messageService.getEventMessageStats(1L);
// Returns: {totalMessages, unreadMessages, inboundMessages, outboundMessages, totalGuests}
```

## Contact & Support

For issues or questions regarding WhatsApp integration:

1. Check Meta WhatsApp Cloud API documentation: https://developers.facebook.com/docs/whatsapp/cloud-api
2. Review application logs with DEBUG level enabled
3. Verify Meta Webhook setup in App Dashboard
4. Test webhook with webhook testing tools

