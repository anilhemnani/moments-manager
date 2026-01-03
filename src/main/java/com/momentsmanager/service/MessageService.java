package com.momentsmanager.service;

import com.momentsmanager.model.GuestMessage;
import com.momentsmanager.model.WeddingEvent;
import com.momentsmanager.model.Guest;
import com.momentsmanager.repository.GuestMessageRepository;
import com.momentsmanager.repository.GuestRepository;
import com.momentsmanager.repository.WeddingEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing guest messages
 */
@Service
@Transactional
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    @Autowired
    private GuestMessageRepository guestMessageRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    /**
     * Get all messages for an event, paginated
     */
    public Page<GuestMessage> getEventMessages(Long eventId, Pageable pageable) {
        WeddingEvent event = weddingEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));
        return guestMessageRepository.findByEventOrderByCreatedAtDesc(event, pageable);
    }

    /**
     * Get unread messages for an event
     */
    public List<GuestMessage> getUnreadMessages(Long eventId) {
        WeddingEvent event = weddingEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));
        return guestMessageRepository.findByEventAndIsReadFalse(event);
    }

    /**
     * Get unread message count for an event
     */
    public long getUnreadCount(Long eventId) {
        WeddingEvent event = weddingEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));
        return guestMessageRepository.countByEventAndIsReadFalse(event);
    }

    /**
     * Get all messages from a specific guest in an event, paginated
     */
    public Page<GuestMessage> getGuestMessages(Long eventId, Long guestId, Pageable pageable) {
        WeddingEvent event = weddingEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));
        Guest guest = guestRepository.findById(guestId)
            .orElseThrow(() -> new RuntimeException("Guest not found"));

        List<GuestMessage> messages = guestMessageRepository.findByGuestAndEventOrderByCreatedAtDesc(guest, event);
        return convertListToPage(messages, pageable);
    }

    /**
     * Get all inbound messages (from guests) for an event, paginated
     */
    public Page<GuestMessage> getInboundMessages(Long eventId, Pageable pageable) {
        WeddingEvent event = weddingEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));
        return guestMessageRepository.findAllInboundMessages(event, pageable);
    }

    /**
     * Get unread inbound messages for an event
     */
    public List<GuestMessage> getUnreadInboundMessages(Long eventId) {
        WeddingEvent event = weddingEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));
        return guestMessageRepository.findUnreadInboundMessages(event);
    }

    /**
     * Get message by ID
     */
    public GuestMessage getMessage(Long messageId) {
        return guestMessageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found"));
    }

    /**
     * Mark message as read
     */
    public GuestMessage markAsRead(Long messageId) {
        GuestMessage message = getMessage(messageId);
        message.markAsRead();
        return guestMessageRepository.save(message);
    }

    /**
     * Mark message as unread
     */
    public GuestMessage markAsUnread(Long messageId) {
        GuestMessage message = getMessage(messageId);
        message.markAsUnread();
        return guestMessageRepository.save(message);
    }

    /**
     * Mark all messages from a guest as read
     */
    public void markGuestMessagesAsRead(Long eventId, Long guestId) {
        WeddingEvent event = weddingEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));
        Guest guest = guestRepository.findById(guestId)
            .orElseThrow(() -> new RuntimeException("Guest not found"));

        List<GuestMessage> unreadMessages = guestMessageRepository
            .findByGuestAndEventAndIsReadFalseOrderByCreatedAtDesc(guest, event);

        unreadMessages.forEach(GuestMessage::markAsRead);
        guestMessageRepository.saveAll(unreadMessages);
    }

    /**
     * Mark all messages in an event as read
     */
    public void markEventMessagesAsRead(Long eventId) {
        WeddingEvent event = weddingEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        List<GuestMessage> unreadMessages = guestMessageRepository.findByEventAndIsReadFalse(event);
        unreadMessages.forEach(GuestMessage::markAsRead);
        guestMessageRepository.saveAll(unreadMessages);
    }

    /**
     * Create and store a new incoming message
     */
    public GuestMessage storeIncomingMessage(Long eventId, String guestPhoneNumber, String messageContent,
                                            String whatsappMessageId, GuestMessage.MessageType messageType,
                                            String mediaUrl) {
        WeddingEvent event = weddingEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        // Try to find guest by phone number
        Guest guest = guestRepository.findByContactPhone(guestPhoneNumber);

        if (guest == null) {
            logger.warn("Message received from unknown phone number: {}", guestPhoneNumber);
            // Create message without guest association (can be assigned manually later)
        }

        GuestMessage message = GuestMessage.builder()
            .event(event)
            .guest(guest)
            .guestPhoneNumber(guestPhoneNumber)
            .messageContent(messageContent)
            .direction(GuestMessage.MessageDirection.INBOUND)
            .messageType(messageType != null ? messageType : GuestMessage.MessageType.TEXT)
            .mediaUrl(mediaUrl)
            .whatsappMessageId(whatsappMessageId)
            .status(GuestMessage.MessageStatus.DELIVERED)
            .isRead(false)
            .createdAt(LocalDateTime.now())
            .build();

        return guestMessageRepository.save(message);
    }

    /**
     * Update message status from WhatsApp webhook
     */
    public GuestMessage updateMessageStatus(String whatsappMessageId, String status) {
        Optional<GuestMessage> messageOpt = guestMessageRepository.findByWhatsappMessageId(whatsappMessageId);

        if (messageOpt.isEmpty()) {
            logger.warn("Received status update for unknown message ID: {}", whatsappMessageId);
            return null;
        }

        GuestMessage message = messageOpt.get();
        try {
            message.setStatus(GuestMessage.MessageStatus.valueOf(status.toUpperCase()));

            // Update read timestamp if status is READ
            if (GuestMessage.MessageStatus.READ.equals(message.getStatus())) {
                message.setReadAt(LocalDateTime.now());
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown message status: {}", status);
        }

        return guestMessageRepository.save(message);
    }

    /**
     * Get conversation between host and guest (all messages in both directions)
     */
    public List<GuestMessage> getConversation(Long eventId, Long guestId) {
        WeddingEvent event = weddingEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));
        Guest guest = guestRepository.findById(guestId)
            .orElseThrow(() -> new RuntimeException("Guest not found"));

        return guestMessageRepository.findByGuestAndEventOrderByCreatedAtDesc(guest, event);
    }

    /**
     * Get grouped messages by guest (for inbox view)
     */
    public Map<Guest, List<GuestMessage>> getMessagesGroupedByGuest(Long eventId) {
        WeddingEvent event = weddingEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        List<GuestMessage> allMessages = guestMessageRepository.findByEventOrderByCreatedAtDesc(event);

        return allMessages.stream()
            .filter(m -> m.getGuest() != null) // Only messages with associated guest
            .collect(Collectors.groupingBy(
                GuestMessage::getGuest,
                Collectors.toList()
            ));
    }

    /**
     * Get guest's unread message count for an event
     */
    public long getGuestUnreadCount(Long eventId, Long guestId) {
        WeddingEvent event = weddingEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));
        Guest guest = guestRepository.findById(guestId)
            .orElseThrow(() -> new RuntimeException("Guest not found"));

        return guestMessageRepository.countByGuestAndEventAndIsReadFalse(guest, event);
    }

    /**
     * Get messages by phone number and event (for unassociated messages)
     */
    public Page<GuestMessage> getMessagesByPhone(Long eventId, String phoneNumber, Pageable pageable) {
        WeddingEvent event = weddingEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        return guestMessageRepository.findByGuestPhoneNumberAndEventOrderByCreatedAtDesc(phoneNumber, event, pageable);
    }

    /**
     * Associate unassociated message with a guest
     */
    public GuestMessage associateMessageWithGuest(Long messageId, Long guestId) {
        GuestMessage message = getMessage(messageId);
        Guest guest = guestRepository.findById(guestId)
            .orElseThrow(() -> new RuntimeException("Guest not found"));

        message.setGuest(guest);
        return guestMessageRepository.save(message);
    }

    /**
     * Get new messages since a specific time
     */
    public List<GuestMessage> getNewMessages(Long eventId, LocalDateTime since) {
        WeddingEvent event = weddingEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        return guestMessageRepository.findNewMessages(event, since);
    }

    /**
     * Delete message
     */
    public void deleteMessage(Long messageId) {
        guestMessageRepository.deleteById(messageId);
    }

    /**
     * Get message statistics for an event
     */
    public Map<String, Object> getEventMessageStats(Long eventId) {
        WeddingEvent event = weddingEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        List<GuestMessage> allMessages = guestMessageRepository.findByEventOrderByCreatedAtDesc(event);
        long unreadCount = allMessages.stream().filter(m -> !m.isRead()).count();
        long inboundCount = allMessages.stream().filter(m ->
            GuestMessage.MessageDirection.INBOUND.equals(m.getDirection())).count();
        long outboundCount = allMessages.stream().filter(m ->
            GuestMessage.MessageDirection.OUTBOUND.equals(m.getDirection())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMessages", allMessages.size());
        stats.put("unreadMessages", unreadCount);
        stats.put("inboundMessages", inboundCount);
        stats.put("outboundMessages", outboundCount);
        stats.put("totalGuests", allMessages.stream()
            .map(GuestMessage::getGuest)
            .filter(Objects::nonNull)
            .distinct()
            .count());

        return stats;
    }

    /**
     * Convert list to Page object (helper method for pagination)
     */
    private Page<GuestMessage> convertListToPage(List<GuestMessage> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());

        List<GuestMessage> pageContent = list.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, list.size());
    }
}

