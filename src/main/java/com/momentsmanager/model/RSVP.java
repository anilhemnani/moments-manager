package com.momentsmanager.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rsvp_tbl")
public class RSVP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "guest_id", unique = true)
    private Guest guest;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "status")
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private RSVPStatus status = RSVPStatus.PENDING;

    @Column(name = "attendee_count")
    private int attendeeCount;

    @OneToMany(mappedBy = "rsvp", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Attendee> attendees = new ArrayList<>();
}
