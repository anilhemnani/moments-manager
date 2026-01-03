package com.momentsmanager.repository;

import com.momentsmanager.model.GuestMessage;
import com.momentsmanager.model.WeddingEvent;
import com.momentsmanager.model.Guest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for GuestMessage entity
 */
@Repository
public interface GuestMessageRepository extends JpaRepository<GuestMessage, Long> {

    /**
     * Find all messages for an event that are unread
     */
    List<GuestMessage> findByEventAndIsReadFalse(WeddingEvent event);

    /**
     * Find all messages for an event, ordered by most recent first
     */
    List<GuestMessage> findByEventOrderByCreatedAtDesc(WeddingEvent event);

    /**
     * Find paginated messages for an event
     */
    Page<GuestMessage> findByEventOrderByCreatedAtDesc(WeddingEvent event, Pageable pageable);

    /**
     * Find all messages between a guest and an event
     */
    List<GuestMessage> findByGuestAndEventOrderByCreatedAtDesc(Guest guest, WeddingEvent event);

    /**
     * Find messages by phone number and event
     */
    List<GuestMessage> findByGuestPhoneNumberAndEventOrderByCreatedAtDesc(String phoneNumber, WeddingEvent event);

    /**
     * Find messages by guest phone and event (paginated)
     */
    Page<GuestMessage> findByGuestPhoneNumberAndEventOrderByCreatedAtDesc(String phoneNumber, WeddingEvent event, Pageable pageable);

    /**
     * Find message by WhatsApp message ID
     */
    Optional<GuestMessage> findByWhatsappMessageId(String whatsappMessageId);

    /**
     * Count unread messages for an event
     */
    long countByEventAndIsReadFalse(WeddingEvent event);

    /**
     * Count unread messages by guest in an event
     */
    long countByGuestAndEventAndIsReadFalse(Guest guest, WeddingEvent event);

    /**
     * Find unread messages for a guest in an event
     */
    List<GuestMessage> findByGuestAndEventAndIsReadFalseOrderByCreatedAtDesc(Guest guest, WeddingEvent event);

    /**
     * Find all inbound messages for an event (messages from guests)
     */
    @Query("SELECT m FROM GuestMessage m WHERE m.event = :event AND m.direction = 'INBOUND' ORDER BY m.createdAt DESC")
    List<GuestMessage> findAllInboundMessages(@Param("event") WeddingEvent event);

    /**
     * Find paginated inbound messages for an event
     */
    @Query("SELECT m FROM GuestMessage m WHERE m.event = :event AND m.direction = 'INBOUND' ORDER BY m.createdAt DESC")
    Page<GuestMessage> findAllInboundMessages(@Param("event") WeddingEvent event, Pageable pageable);

    /**
     * Find unread inbound messages for an event
     */
    @Query("SELECT m FROM GuestMessage m WHERE m.event = :event AND m.direction = 'INBOUND' AND m.isRead = false ORDER BY m.createdAt DESC")
    List<GuestMessage> findUnreadInboundMessages(@Param("event") WeddingEvent event);

    /**
     * Find inbound messages from a specific guest
     */
    @Query("SELECT m FROM GuestMessage m WHERE m.event = :event AND m.guest = :guest AND m.direction = 'INBOUND' ORDER BY m.createdAt DESC")
    List<GuestMessage> findInboundMessagesFromGuest(@Param("event") WeddingEvent event, @Param("guest") Guest guest);

    /**
     * Find messages by event and phone number with direction
     */
    @Query("SELECT m FROM GuestMessage m WHERE m.event = :event AND m.guestPhoneNumber = :phoneNumber ORDER BY m.createdAt DESC")
    List<GuestMessage> findByEventAndPhoneNumber(@Param("event") WeddingEvent event, @Param("phoneNumber") String phoneNumber);

    /**
     * Find messages created after a specific timestamp
     */
    @Query("SELECT m FROM GuestMessage m WHERE m.event = :event AND m.createdAt > :timestamp ORDER BY m.createdAt DESC")
    List<GuestMessage> findNewMessages(@Param("event") WeddingEvent event, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Find messages by status
     */
    @Query("SELECT m FROM GuestMessage m WHERE m.event = :event AND m.status = :status ORDER BY m.createdAt DESC")
    List<GuestMessage> findByEventAndStatus(@Param("event") WeddingEvent event, @Param("status") GuestMessage.MessageStatus status);

    /**
     * Get count of unread inbound messages by guest for an event
     */
    @Query("SELECT m.guest.id, COUNT(m) FROM GuestMessage m WHERE m.event = :event AND m.direction = 'INBOUND' AND m.isRead = false GROUP BY m.guest.id")
    List<Object[]> getUnreadCountByGuest(@Param("event") WeddingEvent event);
}

