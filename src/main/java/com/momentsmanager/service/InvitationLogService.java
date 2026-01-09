package com.momentsmanager.service;

import com.momentsmanager.model.Guest;
import com.momentsmanager.model.Invitation;
import com.momentsmanager.model.InvitationLog;
import com.momentsmanager.model.WeddingEvent;
import com.momentsmanager.repository.GuestRepository;
import com.momentsmanager.repository.InvitationLogRepository;
import com.momentsmanager.repository.InvitationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class InvitationLogService {
    private static final Logger logger = LoggerFactory.getLogger(InvitationLogService.class);

    @Autowired
    private InvitationLogRepository invitationLogRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private WhatsAppService whatsAppService;

    @Transactional
    public List<InvitationLog> sendInvitationToGuests(Long invitationId, List<Long> guestIds, String sentBy) {
        Optional<Invitation> invitationOpt = invitationRepository.findById(invitationId);
        if (invitationOpt.isEmpty()) {
            throw new RuntimeException("Invitation not found with id: " + invitationId);
        }

        Invitation invitation = invitationOpt.get();
        List<InvitationLog> logs = new ArrayList<>();

        // Validate template requirements based on invitation's message type
        if ("TEMPLATE".equals(invitation.getMessageType())) {
            if (invitation.getTemplateName() == null || invitation.getTemplateName().isBlank()) {
                throw new RuntimeException("WhatsApp template name is required when message type is TEMPLATE.");
            }
        }

        for (Long guestId : guestIds) {
            Optional<Guest> guestOpt = guestRepository.findById(guestId);
            if (guestOpt.isEmpty()) {
                logger.warn("Guest not found with id: {}", guestId);
                continue;
            }

            Guest guest = guestOpt.get();

            // Check if invitation already sent to this guest
            Optional<InvitationLog> existingLog = invitationLogRepository.findByInvitationIdAndGuestId(invitationId, guestId);
            if (existingLog.isPresent()) {
                logger.info("Invitation already sent to guest: {}", guestId);
                continue;
            }

            // Create invitation log
            InvitationLog log = InvitationLog.builder()
                    .invitation(invitation)
                    .guest(guest)
                    .sentBy(sentBy)
                    .sentAt(LocalDateTime.now())
                    .whatsappNumber(guest.getContactPhone())
                    .deliveryStatus("PENDING")
                    .build();

            // Send WhatsApp message using invitation's message type and template settings
            try {
                boolean sent = whatsAppService.sendMessage(
                        invitation.getEvent(), // Pass the event for Cloud API configuration
                        guest.getContactPhone(),
                        invitation.getTitle(),
                        invitation.getMessage(),
                        invitation.getImageUrl(),
                        invitation.getMessageType(),
                        invitation.getTemplateName(),
                        invitation.getTemplateLanguage()
                );

                if (sent) {
                    log.setDeliveryStatus("SENT");
                    log.setDeliveryTimestamp(LocalDateTime.now());
                } else {
                    log.setDeliveryStatus("FAILED");
                    log.setErrorMessage("Failed to send WhatsApp message");
                }
            } catch (Exception e) {
                logger.error("Error sending WhatsApp to guest {}: {}", guestId, e.getMessage());
                log.setDeliveryStatus("FAILED");
                log.setErrorMessage(e.getMessage());
            }

            InvitationLog savedLog = invitationLogRepository.save(log);
            logs.add(savedLog);
        }

        return logs;
    }

    @Transactional(readOnly = true)
    public List<InvitationLog> getLogsByInvitation(Long invitationId) {
        return invitationLogRepository.findByInvitationId(invitationId);
    }

    @Transactional(readOnly = true)
    public List<InvitationLog> getLogsByGuest(Long guestId) {
        return invitationLogRepository.findByGuestId(guestId);
    }

    @Transactional(readOnly = true)
    public List<InvitationLog> getLogsByEvent(Long eventId) {
        return invitationLogRepository.findByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public Long getInvitationSentCount(Long invitationId) {
        return invitationLogRepository.countByInvitationId(invitationId);
    }

    @Transactional(readOnly = true)
    public Long getDeliveredCount(Long invitationId) {
        return invitationLogRepository.countByInvitationIdAndStatus(invitationId, "SENT");
    }

    @Transactional(readOnly = true)
    public Long getFailedCount(Long invitationId) {
        return invitationLogRepository.countByInvitationIdAndStatus(invitationId, "FAILED");
    }

    @Transactional
    public void retryFailedDelivery(Long logId) {
        Optional<InvitationLog> logOpt = invitationLogRepository.findById(logId);
        if (logOpt.isEmpty() || !logOpt.get().getDeliveryStatus().equals("FAILED")) {
            return;
        }

        InvitationLog log = logOpt.get();
        try {
            boolean sent = whatsAppService.sendMessage(
                    log.getWhatsappNumber(),
                    log.getInvitation().getTitle(),
                    log.getInvitation().getMessage(),
                    log.getInvitation().getImageUrl()
            );

            if (sent) {
                log.setDeliveryStatus("SENT");
                log.setDeliveryTimestamp(LocalDateTime.now());
                log.setErrorMessage(null);
            }
        } catch (Exception e) {
            logger.error("Retry failed for log {}: {}", logId, e.getMessage());
            log.setErrorMessage("Retry failed: " + e.getMessage());
        }

        invitationLogRepository.save(log);
    }

    /**
     * Mark an invitation as sent externally (email, phone call, in-person, etc.)
     * Creates an invitation log without actually sending via WhatsApp
     */
    @Transactional
    public InvitationLog markInvitationSentExternally(Long invitationId, Long guestId,
                                                       String externalMethod, String sentBy) {
        Optional<Invitation> invitationOpt = invitationRepository.findById(invitationId);
        Optional<Guest> guestOpt = guestRepository.findById(guestId);

        if (invitationOpt.isEmpty()) {
            throw new RuntimeException("Invitation not found with id: " + invitationId);
        }
        if (guestOpt.isEmpty()) {
            throw new RuntimeException("Guest not found with id: " + guestId);
        }

        Invitation invitation = invitationOpt.get();
        Guest guest = guestOpt.get();

        // Check if invitation already sent to this guest
        Optional<InvitationLog> existingLog = invitationLogRepository.findByInvitationIdAndGuestId(invitationId, guestId);
        if (existingLog.isPresent()) {
            logger.info("Invitation already sent to guest: {}", guestId);
            throw new RuntimeException("Invitation already sent to this guest");
        }

        // Create invitation log for external invitation
        InvitationLog log = InvitationLog.builder()
                .invitation(invitation)
                .guest(guest)
                .sentBy(sentBy)
                .sentAt(LocalDateTime.now())
                .deliveryStatus("SENT")
                .deliveryTimestamp(LocalDateTime.now())
                .invitationMethod("EXTERNAL")
                .externalMethodDescription(externalMethod)
                .build();

        InvitationLog savedLog = invitationLogRepository.save(log);
        logger.info("Marked invitation {} as sent externally to guest {} via {}",
                   invitationId, guestId, externalMethod);
        return savedLog;
    }

    /**
     * Mark multiple invitations as sent externally
     */
    @Transactional
    public List<InvitationLog> markMultipleInvitationsSentExternally(Long invitationId, List<Long> guestIds,
                                                                      String externalMethod, String sentBy) {
        List<InvitationLog> logs = new ArrayList<>();
        for (Long guestId : guestIds) {
            try {
                InvitationLog log = markInvitationSentExternally(invitationId, guestId, externalMethod, sentBy);
                logs.add(log);
            } catch (Exception e) {
                logger.warn("Failed to mark invitation as sent externally for guest {}: {}", guestId, e.getMessage());
            }
        }
        return logs;
    }
}
