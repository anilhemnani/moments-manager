# WhatsApp Configuration - Seed Data Update

## Summary

Successfully updated the seed data to enable WhatsApp Cloud API integration with your credentials.

## Changes Applied

### 1. Guest Phone Numbers Updated
All guest contact phone numbers in the seed data have been updated to:
- **Phone Number**: `+447878597720` (UK number)

**Affected Records:**
- Guest 1 (Sharma family)
- Guest 2 (Patel family)
- Host 1
- Host 2

### 2. WhatsApp Configuration Enabled

Event ID 1 ("Ravi & Meera Wedding") now has WhatsApp Cloud API enabled with the following configuration:

| Configuration | Value |
|--------------|-------|
| **WhatsApp API Enabled** | `true` |
| **Business Account ID (WABA_ID)** | `1569365317617857` |
| **Phone Number ID** | `889286674274022` |
| **Access Token** | `EAAhsfeFINq4BQ...` (512 chars) |
| **API Version** | `v18.0` |
| **Verify Token** | `moments-manager-verify-token` |

## Database Migration

**Changeset ID**: `25-update-guest-phones-and-enable-whatsapp`

**Location**: `src/main/resources/db/changelog/db.changelog-master.xml`

This changeset will be automatically applied by Liquibase when the application starts.

## What This Enables

With this configuration, your application can now:

1. ✅ **Send WhatsApp Messages** to guests via Cloud API
2. ✅ **Receive WhatsApp Messages** from guests via webhook
3. ✅ **Track Message Status** (sent, delivered, read)
4. ✅ **Manage Messages** in the host inbox
5. ✅ **Use WhatsApp Templates** from your Meta Business Account

## Next Steps

### 1. Start the Application
```bash
cd /home/anilhemnani/moments-manager
mvn spring-boot:run
```

The Liquibase migration will automatically:
- Update all guest phone numbers to `+447878597720`
- Enable WhatsApp for Event ID 1
- Configure the WhatsApp Cloud API credentials

### 2. Configure Meta Webhook

In your Meta App Dashboard:
1. Go to WhatsApp Business App → Webhooks
2. Add subscription for **messages**
3. Set **Callback URL**: `https://your-domain.com/api/whatsapp/webhook`
4. Set **Verify Token**: `moments-manager-verify-token`
5. Subscribe to fields: `messages`, `message_status`

### 3. Test the Integration

**Send a test message from guest:**
```bash
# Simulate incoming message from +447878597720
curl -X POST http://localhost:8080/api/whatsapp/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "object":"whatsapp_business_account",
    "entry":[{
      "id":"1569365317617857",
      "changes":[{
        "field":"messages",
        "value":{
          "messaging_product":"whatsapp",
          "metadata":{
            "display_phone_number":"889286674274022",
            "phone_number_id":"889286674274022"
          },
          "messages":[{
            "id":"wamid.test.001",
            "from":"447878597720",
            "timestamp":"1640995200",
            "type":"text",
            "text":{"body":"Hello! Looking forward to the wedding!"}
          }]
        }
      }]
    }]
  }'
```

**Check messages in database:**
```sql
-- Using H2 Console at http://localhost:8080/h2-console
SELECT * FROM guest_message_tbl;
```

**View in inbox UI:**
```
http://localhost:8080/inbox/events/1
```

### 4. Send Invitations

You can now send WhatsApp invitations to guests:
1. Navigate to: `http://localhost:8080/invitations/events/1`
2. Create a new invitation
3. Select guests (all will have phone +447878597720)
4. Send via WhatsApp Cloud API

### 5. Verify Guest Data

**Check updated guest data:**
```sql
-- View all guests with updated phone numbers
SELECT id, family_name, contact_name, contact_phone, event_id 
FROM guest_tbl 
WHERE event_id = 1;

-- Expected result:
-- id | family_name | contact_name  | contact_phone    | event_id
-- 1  | Sharma      | Ravi Sharma   | +447878597720   | 1
-- 2  | Patel       | Meera Patel   | +447878597720   | 1
```

**Check WhatsApp configuration:**
```sql
-- View WhatsApp configuration for event
SELECT id, name, whatsapp_api_enabled, whatsapp_phone_number_id, 
       whatsapp_business_account_id, whatsapp_api_version 
FROM wedding_event_tbl 
WHERE id = 1;

-- Expected result:
-- whatsapp_api_enabled: true
-- whatsapp_phone_number_id: 889286674274022
-- whatsapp_business_account_id: 1569365317617857
-- whatsapp_api_version: v18.0
```

## Configuration Details

### WhatsApp Cloud API Credentials

Your WhatsApp Business Account is now configured with:

```yaml
Business Account:
  WABA ID: 1569365317617857
  
Phone Number:
  Phone Number ID: 889286674274022
  Display Number: +447878597720 (configured in Meta)
  
Authentication:
  Access Token: EAAhsfeFINq4BQ... (expires based on Meta settings)
  API Version: v18.0
  
Webhook:
  Verify Token: moments-manager-verify-token
  Endpoint: /api/whatsapp/webhook
```

### Important Notes

1. **Access Token Expiry**: The access token may expire. Check Meta Business Manager to generate a new token if needed.

2. **Phone Number Matching**: All guests now have the same phone number (`+447878597720`). In production:
   - Each guest should have a unique phone number
   - Messages will be matched to guests by their phone number
   - Consider updating guest phone numbers to real values

3. **Message Routing**: Since all guests share the same phone:
   - Incoming messages from `+447878597720` will match multiple guests
   - The system will store the message but may not uniquely identify the guest
   - For testing, this is fine; for production, use unique phone numbers

4. **Security**: 
   - The access token is stored in the database
   - Consider encryption at rest for production
   - Rotate tokens periodically

## Verification Checklist

- ✅ Liquibase changeset created (id: 25)
- ✅ Guest phone numbers will be updated to +447878597720
- ✅ Host phone numbers will be updated to +447878597720
- ✅ WhatsApp API enabled for Event ID 1
- ✅ WhatsApp credentials configured
- ✅ Application compiles successfully
- ⏳ Pending: Start application to apply migration
- ⏳ Pending: Configure Meta webhook
- ⏳ Pending: Test message sending/receiving

## Database Migration Preview

When the application starts, Liquibase will execute:

```sql
-- Update guest phone numbers
UPDATE guest_tbl 
SET contact_phone = '+447878597720' 
WHERE event_id = 1;

-- Update host phone numbers
UPDATE host_tbl 
SET phone = '+447878597720' 
WHERE event_id = 1;

-- Enable and configure WhatsApp
UPDATE wedding_event_tbl 
SET whatsapp_api_enabled = true,
    whatsapp_phone_number_id = '889286674274022',
    whatsapp_business_account_id = '1569365317617857',
    whatsapp_access_token = 'EAAhsfeFINq4BQ...',
    whatsapp_api_version = 'v18.0',
    whatsapp_verify_token = 'moments-manager-verify-token'
WHERE id = 1;
```

## Files Modified

- ✅ `src/main/resources/db/changelog/db.changelog-master.xml`
  - Added changeset 25: Update guest phones and enable WhatsApp

## Support

If you encounter any issues:

1. **Check Liquibase logs** when starting the application
2. **Verify in H2 Console** that updates were applied
3. **Review application logs** for any errors
4. **Test webhook endpoint** with curl commands above
5. **Check Meta Business Manager** for access token validity

---

**Status**: ✅ Configuration Complete - Ready to Start Application

**Next Action**: Start the application with `mvn spring-boot:run` to apply the migration.

