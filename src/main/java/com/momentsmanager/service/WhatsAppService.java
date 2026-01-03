package com.momentsmanager.service;

import com.momentsmanager.model.WeddingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * WhatsApp Service for sending messages
 * Supports both WhatsApp Cloud API and fallback URL scheme
 */
@Service
public class WhatsAppService {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send a WhatsApp message using event's Cloud API configuration with invitation's template settings
     *
     * @param event The wedding event with WhatsApp configuration
     * @param phoneNumber Phone number (with country code)
     * @param title Message title
     * @param message Message content
     * @param imageUrl Optional image URL
     * @param messageType Message type (TEMPLATE or PLAIN_TEXT)
     * @param templateName Template name (required if messageType is TEMPLATE)
     * @param templateLanguage Template language code
     * @return true if message was sent successfully
     */
    public boolean sendMessage(WeddingEvent event, String phoneNumber, String title, String message, String imageUrl,
                              String messageType, String templateName, String templateLanguage) {
        try {
            String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");

            // If event is configured for API and message type is TEMPLATE, send template
            if (event != null && Boolean.TRUE.equals(event.getWhatsappApiEnabled()) &&
                "TEMPLATE".equals(messageType) && templateName != null && !templateName.isEmpty()) {
                return sendTemplateViaCloudAPI(event, cleanNumber, title, message, imageUrl, templateName, templateLanguage);
            }

            // Otherwise, send plain text message
            StringBuilder fullMessage = new StringBuilder();
            fullMessage.append("*").append(title).append("*\n\n");
            fullMessage.append(message);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                fullMessage.append("\n\n").append(imageUrl);
            }

            if (event != null && Boolean.TRUE.equals(event.getWhatsappApiEnabled())) {
                return sendViaCloudAPI(event, cleanNumber, fullMessage.toString(), imageUrl);
            } else {
                logger.info("WhatsApp message queued for {}: {}", cleanNumber, title);
                logger.debug("Full message: {}", fullMessage.toString());
                logger.info("WhatsApp Cloud API not configured. Message logged but not sent.");
                return true;
            }
        } catch (Exception e) {
            logger.error("Failed to send WhatsApp message: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Send a WhatsApp message to a phone number
     *
     * @param phoneNumber Phone number (with country code)
     * @param title Message title
     * @param message Message content
     * @param imageUrl Optional image URL
     * @return true if message was queued successfully
     */
    public boolean sendMessage(String phoneNumber, String title, String message, String imageUrl) {
        return sendMessage(null, phoneNumber, title, message, imageUrl);
    }

    /**
     * Send a WhatsApp message using event's Cloud API configuration
     *
     * @param event The wedding event with WhatsApp configuration
     * @param phoneNumber Phone number (with country code)
     * @param title Message title
     * @param message Message content
     * @param imageUrl Optional image URL
     * @return true if message was sent successfully
     */
    public boolean sendMessage(WeddingEvent event, String phoneNumber, String title, String message, String imageUrl) {
        // Delegate to the new overloaded method with PLAIN_TEXT as default
        return sendMessage(event, phoneNumber, title, message, imageUrl, "PLAIN_TEXT", null, "en_US");
    }

    /**
     * Send message via WhatsApp Cloud API
     */
    private boolean sendViaCloudAPI(WeddingEvent event, String phoneNumber, String message, String imageUrl) {
        try {
            String apiUrl = String.format("https://graph.facebook.com/%s/%s/messages",
                    event.getWhatsappApiVersion(),
                    event.getWhatsappPhoneNumberId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(event.getWhatsappAccessToken());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messaging_product", "whatsapp");
            requestBody.put("to", phoneNumber);

            // Text message
            Map<String, Object> textMessage = new HashMap<>();
            textMessage.put("preview_url", true);
            textMessage.put("body", message);

            requestBody.put("type", "text");
            requestBody.put("text", textMessage);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Send the request
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("WhatsApp message sent successfully via Cloud API to {}", phoneNumber);
                return true;
            } else {
                logger.error("Failed to send WhatsApp message. Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error sending message via WhatsApp Cloud API: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean sendTemplateViaCloudAPI(WeddingEvent event, String phoneNumber, String title, String message, String imageUrl, String templateName, String templateLanguage) {
        try {
            String apiUrl = String.format("https://graph.facebook.com/%s/%s/messages",
                    event.getWhatsappApiVersion(),
                    event.getWhatsappPhoneNumberId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(event.getWhatsappAccessToken());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messaging_product", "whatsapp");
            requestBody.put("to", phoneNumber);
            requestBody.put("type", "template");

            Map<String, Object> template = new HashMap<>();
            template.put("name", templateName);
            template.put("language", Map.of("code", templateLanguage != null ? templateLanguage : "en_US"));

            // Note: WhatsApp templates are sent exactly as configured in Meta Business Manager
            // Do not add parameters unless the template was specifically created with placeholders
            // For templates with placeholders, parameters would need to be added here in components array

            requestBody.put("template", template);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            logger.debug("Sending WhatsApp template message: template={}, language={}, to={}",
                templateName, templateLanguage, phoneNumber);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("WhatsApp template message sent successfully via Cloud API to {}", phoneNumber);
                return true;
            } else {
                logger.error("Failed to send WhatsApp template message. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("HTTP Error sending template message: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            logger.error("Template: {}, Language: {}", templateName, templateLanguage);
            logger.error("Note: Ensure template exists and is approved in Meta Business Manager");
            return false;
        } catch (Exception e) {
            logger.error("Error sending template message via WhatsApp Cloud API: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate WhatsApp Web URL for manual sending
     * This can be used to create a link that opens WhatsApp with pre-filled message
     *
     * @param phoneNumber Phone number
     * @param message Message content
     * @return WhatsApp Web URL
     */
    public String generateWhatsAppUrl(String phoneNumber, String message) {
        try {
            String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");
            String encodedMessage = URLEncoder.encode(message, "UTF-8");
            return "https://wa.me/" + cleanNumber + "?text=" + encodedMessage;
        } catch (UnsupportedEncodingException e) {
            logger.error("Failed to encode WhatsApp URL: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Fetch available WhatsApp templates from Meta API for the event's business account
     *
     * @param event The wedding event with WhatsApp configuration
     * @return List of template names, or empty list if API not configured or error occurs
     */
    public java.util.List<Map<String, String>> fetchAvailableTemplates(WeddingEvent event) {
        java.util.List<Map<String, String>> templates = new java.util.ArrayList<>();

        if (event == null || !Boolean.TRUE.equals(event.getWhatsappApiEnabled())) {
            logger.debug("WhatsApp API not enabled for event, cannot fetch templates");
            return templates;
        }

        // Validate required fields
        if (event.getWhatsappBusinessAccountId() == null || event.getWhatsappBusinessAccountId().isEmpty()) {
            logger.warn("WhatsApp Business Account ID is not configured for event. Cannot fetch templates.");
            logger.debug("Available IDs - Business Account: {}, Phone Number: {}",
                event.getWhatsappBusinessAccountId(), event.getWhatsappPhoneNumberId());
            return templates;
        }

        if (event.getWhatsappAccessToken() == null || event.getWhatsappAccessToken().isEmpty()) {
            logger.warn("WhatsApp Access Token is not configured for event. Cannot fetch templates.");
            return templates;
        }

        try {
            String businessAccountId = event.getWhatsappBusinessAccountId().trim();
            String apiVersion = event.getWhatsappApiVersion() != null ? event.getWhatsappApiVersion() : "v18.0";

            String apiUrl = String.format("https://graph.facebook.com/%s/%s/message_templates",
                    apiVersion, businessAccountId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(event.getWhatsappAccessToken());

            HttpEntity<String> entity = new HttpEntity<>(headers);

            logger.debug("Fetching WhatsApp templates from Business Account {} using API version {}", businessAccountId, apiVersion);
            logger.debug("API URL: {}", apiUrl);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                templates = parseTemplatesFromResponse(response.getBody());
                logger.info("Successfully fetched {} templates from WhatsApp API for Business Account {}",
                    templates.size(), businessAccountId);
            } else {
                logger.error("Failed to fetch templates. Status: {}, Response: {}",
                    response.getStatusCode(), response.getBody());
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("HTTP Error fetching WhatsApp templates: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            logger.error("Ensure that whatsappBusinessAccountId is set correctly (not a phone number ID)");
            logger.error("Error details: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching WhatsApp templates: {}", e.getMessage(), e);
        }

        return templates;
    }

    /**
     * Parse WhatsApp API response to extract template information
     *
     * @param responseBody JSON response from WhatsApp API
     * @return List of templates with name and language
     */
    private java.util.List<Map<String, String>> parseTemplatesFromResponse(String responseBody) {
        java.util.List<Map<String, String>> templates = new java.util.ArrayList<>();

        try {
            // Using simple JSON parsing - in production, consider using Jackson or Gson
            // Expected response format: {"data": [{"name": "template_name", "status": "APPROVED", "language": "en_US"}, ...]}

            if (responseBody.contains("\"data\"")) {
                // Extract data array
                int dataStart = responseBody.indexOf("\"data\"");
                int arrayStart = responseBody.indexOf("[", dataStart);
                int arrayEnd = responseBody.lastIndexOf("]");

                if (arrayStart > -1 && arrayEnd > arrayStart) {
                    String dataArray = responseBody.substring(arrayStart + 1, arrayEnd);

                    // Split by object delimiter
                    String[] objects = dataArray.split("\\},\\s*\\{");

                    for (String obj : objects) {
                        // Clean up the object string
                        obj = obj.replaceAll("^\\{", "").replaceAll("\\}$", "");

                        // Extract template name
                        String name = extractJsonValue(obj, "\"name\"");
                        String status = extractJsonValue(obj, "\"status\"");
                        String language = extractJsonValue(obj, "\"language\"");

                        // Only add approved templates
                        if (name != null && !name.isEmpty() && "APPROVED".equals(status)) {
                            Map<String, String> template = new HashMap<>();
                            template.put("name", name);
                            template.put("language", language != null ? language : "en_US");
                            templates.add(template);
                            logger.debug("Found template: {} ({})", name, language);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing WhatsApp templates response: {}", e.getMessage(), e);
        }

        return templates;
    }

    /**
     * Helper method to extract JSON value
     */
    private String extractJsonValue(String jsonString, String key) {
        try {
            int keyIndex = jsonString.indexOf(key);
            if (keyIndex == -1) return null;

            int colonIndex = jsonString.indexOf(":", keyIndex);
            int quoteStart = jsonString.indexOf("\"", colonIndex);
            int quoteEnd = jsonString.indexOf("\"", quoteStart + 1);

            if (quoteStart != -1 && quoteEnd != -1) {
                return jsonString.substring(quoteStart + 1, quoteEnd);
            }
        } catch (Exception e) {
            logger.debug("Error extracting JSON value for key {}: {}", key, e.getMessage());
        }
        return null;
    }

    /**
     * Validate phone number format for WhatsApp (international format)
     *
     * @param phoneNumber Phone number to validate
     * @return true if valid
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }

        // Remove all whitespace, hyphens, parentheses
        String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");

        // Validation for international format:
        // - Should start with + (optional but recommended)
        // - Should have at least 10 digits (minimum for most countries)
        // - Should not exceed 15 digits (ITU-T E.164 standard)

        if (cleanNumber.startsWith("+")) {
            // With country code: +XX XXXXXXXXXX (11-16 chars including +)
            return cleanNumber.length() >= 11 && cleanNumber.length() <= 16;
        } else {
            // Without +: at least 10 digits, max 15
            return cleanNumber.length() >= 10 && cleanNumber.length() <= 15;
        }
    }

    /**
     * Format phone number for WhatsApp API (E.164 format)
     * Removes all formatting and ensures it starts with country code
     *
     * @param phoneNumber Phone number to format
     * @return Formatted phone number (digits only, with leading +)
     */
    public String formatPhoneNumberForWhatsApp(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return phoneNumber;
        }

        // Remove all non-digit characters except +
        String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");

        // Ensure it starts with + for international format
        if (!cleanNumber.startsWith("+")) {
            logger.warn("Phone number {} does not have country code. Consider adding + prefix.", phoneNumber);
        }

        return cleanNumber;
    }
}

