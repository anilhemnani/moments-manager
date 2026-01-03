package com.momentsmanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to store WhatsApp messages exchanged between guests and hosts
 * Handles both inbound messages from guests and outbound messages to guests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "guest_message_tbl", indexes = {
    @Index(name = "idx_event_is_read", columnList = "event_id, is_read"),
    @Index(name = "idx_guest_event", columnList = "guest_id, event_id"),
    @Index(name = "idx_timestamp", columnList = "created_at")
})
public class GuestMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference to the event
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private WeddingEvent event;

    // Reference to the guest (sender of the message)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    // Phone number of the guest (stored for reference, can be matched with guest later)
    @Column(name = "guest_phone_number", nullable = false)
    private String guestPhoneNumber;

    // Message content
    @Column(name = "message_content", columnDefinition = "TEXT")
    private String messageContent;

    // Message direction: INBOUND (from guest) or OUTBOUND (to guest)
    @Column(name = "direction", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageDirection direction;

    // Message type: TEXT, IMAGE, DOCUMENT, etc.
    @Column(name = "message_type")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    // Media URL (if message contains media)
    @Column(name = "media_url")
    private String mediaUrl;

    // Read/unread status (for hosts viewing messages from guests)
    @Column(name = "is_read")
    @Builder.Default
    private boolean isRead = false;

    // WhatsApp message ID (from Meta API)
    @Column(name = "whatsapp_message_id")
    private String whatsappMessageId;

    // Message status: PENDING, SENT, DELIVERED, READ, FAILED
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageStatus status = MessageStatus.PENDING;

    // Error message if status is FAILED
    @Column(name = "error_message")
    private String errorMessage;

    // Timestamp when message was created/received
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Timestamp when message status was last updated
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Mark when host read the message
    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Mark message as read
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * Mark message as unread
     */
    public void markAsUnread() {
        this.isRead = false;
        this.readAt = null;
    }

    // Enum for message direction
    public enum MessageDirection {
        INBOUND,  // Message from guest to host
        OUTBOUND  // Message from host to guest
    }

    // Enum for message type
    public enum MessageType {
        TEXT,
        IMAGE,
        DOCUMENT,
        AUDIO,
        VIDEO,
        LOCATION,
        CONTACT,
        UNKNOWN
    }

    // Enum for message status
    public enum MessageStatus {
        PENDING,    // Message created but not yet processed
        SENT,       // Message sent via WhatsApp
        DELIVERED,  // Message delivered to recipient
        READ,       // Message read by recipient
        FAILED      // Message failed to send/receive
    }
}

