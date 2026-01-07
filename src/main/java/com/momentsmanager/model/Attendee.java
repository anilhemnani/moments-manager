package com.momentsmanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendee_tbl")
public class Attendee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "age_group")
    private String ageGroup; // "Adult" or "Child"

    @ManyToOne
    @JoinColumn(name = "rsvp_id")
    private RSVP rsvp;
}
