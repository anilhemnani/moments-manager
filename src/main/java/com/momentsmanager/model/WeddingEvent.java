package com.momentsmanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Entity
@Table(name = "wedding_event_tbl")
public class WeddingEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Aggregation: Event owns Hosts - cascade all operations, orphan removal enabled
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Host> hosts = new ArrayList<>();

    // Aggregation: Event owns Guests - cascade all operations, orphan removal enabled
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Guest> guests = new ArrayList<>();

    // Aggregation: Event owns Invitations - cascade all operations, orphan removal enabled
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Invitation> invitations = new ArrayList<>();

    private String name;

    // Use LocalDate for the event date
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    private String status;

    @Column(name = "bride_name")
    private String brideName;

    @Column(name = "groom_name")
    private String groomName;

    @Column(name = "subdomain", unique = true, nullable = false, length = 50)
    private String subdomain;

    @Column(name = "expected_guest_arrival_date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expectedGuestArrivalDate;

    @Column(name = "expected_guest_departure_date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expectedGuestDepartureDate;

    @Column(name = "preferred_travel_airport")
    private String preferredTravelAirport;

    @Column(name = "preferred_travel_station")
    private String preferredTravelStation;

    @Column(name = "preferred_expected_attendees")
    private Integer preferredExpectedAttendees;

    private String place;

    @Column(name = "expected_max_attendees")
    private Integer expectedMaxAttendees;

    @Column(name = "expected_attendees")
    private Integer expectedAttendees;

    // WhatsApp Cloud API Configuration
    @Column(name = "whatsapp_api_enabled")
    private Boolean whatsappApiEnabled = false;

    @Column(name = "whatsapp_phone_number_id")
    private String whatsappPhoneNumberId;

    @Column(name = "whatsapp_business_account_id")
    private String whatsappBusinessAccountId;

    @Column(name = "whatsapp_access_token")
    private String whatsappAccessToken;

    @Column(name = "whatsapp_api_version")
    private String whatsappApiVersion = "v24.0";

    @Column(name = "whatsapp_verify_token")
    private String whatsappVerifyToken;

}
