package com.momentsmanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invitation_log_tbl")
public class InvitationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id", nullable = false)
    private Invitation invitation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "sent_by")
    private String sentBy; // Host username who sent it

    @Column(name = "delivery_status")
    @Builder.Default
    private String deliveryStatus = "PENDING"; // PENDING, SENT, DELIVERED, FAILED

    @Column(name = "whatsapp_number")
    private String whatsappNumber; // Phone number used for sending

    @Column(name = "error_message")
    private String errorMessage; // If delivery failed

    @Column(name = "delivery_timestamp")
    private LocalDateTime deliveryTimestamp;

    @Column(name = "invitation_method")
    @Builder.Default
    private String invitationMethod = "WHATSAPP"; // WHATSAPP or EXTERNAL (email, phone call, in-person, etc.)

    @Column(name = "external_method_description")
    private String externalMethodDescription; // Description of how external invitation was sent (e.g., "Email", "Phone Call", "In-person")

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}

