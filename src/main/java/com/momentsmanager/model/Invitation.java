package com.momentsmanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invitation_tbl")
public class Invitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private WeddingEvent event;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message; // Optional for TEMPLATE type, required for PLAIN_TEXT

    @Column(name = "invitation_type")
    private String invitationType; // SAVE_THE_DATE, MAIN_INVITATION, REMINDER, THANK_YOU

    @Column(name = "image_url")
    private String imageUrl; // Optional image/card URL

    @Column(name = "message_type")
    @Builder.Default
    private String messageType = "TEMPLATE"; // PLAIN_TEXT or TEMPLATE

    @Column(name = "template_name")
    private String templateName; // WhatsApp template name (required when messageType is TEMPLATE)

    @Column(name = "template_language")
    @Builder.Default
    private String templateLanguage = "en_US"; // Template language code

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy; // Host username who created it

    @Column(name = "status")
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, ACTIVE, ARCHIVED

    // Track which guests received this invitation
    @OneToMany(mappedBy = "invitation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvitationLog> invitationLogs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

