package com.momentsmanager.repository;

import com.momentsmanager.model.InvitationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationLogRepository extends JpaRepository<InvitationLog, Long> {
    List<InvitationLog> findByInvitationId(Long invitationId);
    List<InvitationLog> findByGuestId(Long guestId);
    Optional<InvitationLog> findByInvitationIdAndGuestId(Long invitationId, Long guestId);

    @Query("SELECT il FROM InvitationLog il WHERE il.invitation.event.id = :eventId")
    List<InvitationLog> findByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(il) FROM InvitationLog il WHERE il.invitation.id = :invitationId")
    Long countByInvitationId(@Param("invitationId") Long invitationId);

    @Query("SELECT COUNT(il) FROM InvitationLog il WHERE il.invitation.id = :invitationId AND il.deliveryStatus = :status")
    Long countByInvitationIdAndStatus(@Param("invitationId") Long invitationId, @Param("status") String status);

    /**
     * Find all invitation logs for a guest by phone number
     */
    @Query("SELECT il FROM InvitationLog il WHERE il.whatsappNumber = :phoneNumber")
    List<InvitationLog> findByGuestPhoneNumber(@Param("phoneNumber") String phoneNumber);

    /**
     * Find invitation log by invitation ID and guest phone number
     */
    @Query("SELECT il FROM InvitationLog il WHERE il.invitation.id = :invitationId AND il.whatsappNumber = :phoneNumber")
    Optional<InvitationLog> findByInvitationIdAndGuestPhoneNumber(@Param("invitationId") Long invitationId, @Param("phoneNumber") String phoneNumber);
}

