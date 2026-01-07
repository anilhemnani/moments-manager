# WhatsApp Webhook Quick Reference

## TL;DR - Quick Answer

### Webhook Callback URL
```
https://your-domain.com/api/whatsapp/webhook
```

### Examples
- **Local:** `http://localhost:8080/api/whatsapp/webhook`
- **Production:** `https://moments-manager.com/api/whatsapp/webhook`

### Key Points
✅ **Single endpoint** for all events  
✅ **Event context** comes from phone_number_id in the webhook payload  
✅ **Event-specific config** stored in `wedding_event_tbl` table  
✅ **Uses both GET and POST** (GET for verification, POST for events)  
✅ **Signature validation** recommended for security  

---

## For Each Wedding Event

You need to configure in the database:

```
whatsapp_api_enabled = true
whatsapp_phone_number_id = "YOUR_PHONE_NUMBER_ID"
whatsapp_business_account_id = "YOUR_BUSINESS_ACCOUNT_ID"  
whatsapp_access_token = "YOUR_ACCESS_TOKEN"
whatsapp_api_version = "v24.0"
whatsapp_verify_token = "your-verification-token"
```

---

## Meta Configuration

1. Webhook URL: `https://your-domain.com/api/whatsapp/webhook`
2. Verification Token: Must match `whatsapp.webhook.verify-token` in app config
3. Subscribe to: `messages`, `message_status`
4. Required: App Secret for signature validation

---

## Test Command

```bash
curl -X GET "http://localhost:8080/api/whatsapp/webhook?hub.mode=subscribe&hub.challenge=test123&hub.verify_token=moments-manager-verify-token"
```

Expected response: `test123`

---

## Important Files

- **Controller:** `WhatsAppWebhookController.java`
- **Config:** `application.yml` (whatsapp section)
- **Database:** `wedding_event_tbl` (whatsapp columns)

---

See `WHATSAPP_WEBHOOK_URL.md` for complete details.

