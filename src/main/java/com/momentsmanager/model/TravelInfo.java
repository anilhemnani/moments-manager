package com.momentsmanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "travel_info_tbl")
public class TravelInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "guest_id", unique = true)
    private Guest guest;

    @Column(name = "arrival_mode")
    @Enumerated(EnumType.STRING)
    private ModeOfTravel arrivalMode; // Flight, Train, Car, Bus, etc.

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(name = "arrival_date_time")
    private LocalDateTime arrivalDateTime;

    // Backwards-compatible field names used by service layer
    @Column(name = "arrival_flight_number")
    private String arrivalFlightNumber;

    @Column(name = "arrival_train_number")
    private String arrivalTrainNumber;

    @Column(name = "arrival_airport")
    private String arrivalAirport;

    @Column(name = "arrival_station")
    private String arrivalStation;

    @Column(name = "departure_mode")
    @Enumerated(EnumType.STRING)
    private ModeOfTravel departureMode; // Flight, Train, Car, Bus, etc.

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(name = "departure_date_time")
    private LocalDateTime departureDateTime;

    @Column(name = "departure_flight_number")
    private String departureFlightNumber;

    @Column(name = "departure_train_number")
    private String departureTrainNumber;

    @Column(name = "departure_airport")
    private String departureAirport;

    @Column(name = "departure_station")
    private String departureStation;

    @Column(name = "needs_pickup")
    private Boolean needsPickup = false;

    @Column(name = "needs_drop")
    private Boolean needsDrop = false;

    @Column(name = "special_requirements")
    private String specialRequirements;

    @Column(name = "notes")
    private String notes;
}
