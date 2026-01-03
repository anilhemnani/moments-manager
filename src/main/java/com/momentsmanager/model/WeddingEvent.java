package com.momentsmanager.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

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
    private String date;
    private String status;
    private String bride_name;
    private String groom_name;
    private String expectedGuestArrivalDateTime;
    private String expectedGuestDepartureDateTime;
    private String preferredAirportArrival;
    private String preferredStationArrival;
    private Integer preferredExpectedAttendees;
    private String place;
    private String expectedArrivalDateTime;
    private String expectedDepartureDateTime;
    private Integer expectedMaxAttendees;
    private Integer expectedAttendees;
    private Boolean expectedAttendanceYes;
    private Boolean expectedAttendanceNo;
    private Boolean expectedAttendanceMayBe;
    private Boolean expectedAttendanceToBeInvited;

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


    public WeddingEvent() {}
    public WeddingEvent(Long id, String name, String date, String status, String bride_name, String groom_name,
                        String expectedGuestArrivalDateTime, String expectedGuestDepartureDateTime,
                        String preferredAirportArrival, String preferredStationArrival,
                        Integer preferredExpectedAttendees, String place,
                        String expectedArrivalDateTime, String expectedDepartureDateTime,
                        Integer expectedMaxAttendees, Integer expectedAttendees,
                        Boolean expectedAttendanceYes, Boolean expectedAttendanceNo,
                        Boolean expectedAttendanceMayBe, Boolean expectedAttendanceToBeInvited) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.status = status;
        this.bride_name = bride_name;
        this.groom_name = groom_name;
        this.expectedGuestArrivalDateTime = expectedGuestArrivalDateTime;
        this.expectedGuestDepartureDateTime = expectedGuestDepartureDateTime;
        this.preferredAirportArrival = preferredAirportArrival;
        this.preferredStationArrival = preferredStationArrival;
        this.preferredExpectedAttendees = preferredExpectedAttendees;
        this.place = place;
        this.expectedArrivalDateTime = expectedArrivalDateTime;
        this.expectedDepartureDateTime = expectedDepartureDateTime;
        this.expectedMaxAttendees = expectedMaxAttendees;
        this.expectedAttendees = expectedAttendees;
        this.expectedAttendanceYes = expectedAttendanceYes;
        this.expectedAttendanceNo = expectedAttendanceNo;
        this.expectedAttendanceMayBe = expectedAttendanceMayBe;
        this.expectedAttendanceToBeInvited = expectedAttendanceToBeInvited;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBrideName() { return bride_name; }
    public void setBrideName(String bride_name) { this.bride_name = bride_name; }
    public String getGroomName() { return groom_name; }
    public void setGroomName(String groom_name) { this.groom_name = groom_name; }
    public String getExpectedGuestArrivalDateTime() { return expectedGuestArrivalDateTime; }
    public void setExpectedGuestArrivalDateTime(String expectedGuestArrivalDateTime) { this.expectedGuestArrivalDateTime = expectedGuestArrivalDateTime; }
    public String getExpectedGuestDepartureDateTime() { return expectedGuestDepartureDateTime; }
    public void setExpectedGuestDepartureDateTime(String expectedGuestDepartureDateTime) { this.expectedGuestDepartureDateTime = expectedGuestDepartureDateTime; }
    public String getPreferredAirportArrival() { return preferredAirportArrival; }
    public void setPreferredAirportArrival(String preferredAirportArrival) { this.preferredAirportArrival = preferredAirportArrival; }
    public String getPreferredStationArrival() { return preferredStationArrival; }
    public void setPreferredStationArrival(String preferredStationArrival) { this.preferredStationArrival = preferredStationArrival; }
    public Integer getPreferredExpectedAttendees() { return preferredExpectedAttendees; }
    public void setPreferredExpectedAttendees(Integer preferredExpectedAttendees) { this.preferredExpectedAttendees = preferredExpectedAttendees; }
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }
    public String getExpectedArrivalDateTime() { return expectedArrivalDateTime; }
    public void setExpectedArrivalDateTime(String expectedArrivalDateTime) { this.expectedArrivalDateTime = expectedArrivalDateTime; }
    public String getExpectedDepartureDateTime() { return expectedDepartureDateTime; }
    public void setExpectedDepartureDateTime(String expectedDepartureDateTime) { this.expectedDepartureDateTime = expectedDepartureDateTime; }
    public Integer getExpectedMaxAttendees() { return expectedMaxAttendees; }
    public void setExpectedMaxAttendees(Integer expectedMaxAttendees) { this.expectedMaxAttendees = expectedMaxAttendees; }
    public Integer getExpectedAttendees() { return expectedAttendees; }
    public void setExpectedAttendees(Integer expectedAttendees) { this.expectedAttendees = expectedAttendees; }
    public Boolean getExpectedAttendanceYes() { return expectedAttendanceYes; }
    public void setExpectedAttendanceYes(Boolean expectedAttendanceYes) { this.expectedAttendanceYes = expectedAttendanceYes; }
    public Boolean getExpectedAttendanceNo() { return expectedAttendanceNo; }
    public void setExpectedAttendanceNo(Boolean expectedAttendanceNo) { this.expectedAttendanceNo = expectedAttendanceNo; }
    public Boolean getExpectedAttendanceMayBe() { return expectedAttendanceMayBe; }
    public void setExpectedAttendanceMayBe(Boolean expectedAttendanceMayBe) { this.expectedAttendanceMayBe = expectedAttendanceMayBe; }
    public Boolean getExpectedAttendanceToBeInvited() { return expectedAttendanceToBeInvited; }
    public void setExpectedAttendanceToBeInvited(Boolean expectedAttendanceToBeInvited) { this.expectedAttendanceToBeInvited = expectedAttendanceToBeInvited; }

    // WhatsApp Configuration getters and setters
    public Boolean getWhatsappApiEnabled() { return whatsappApiEnabled; }
    public void setWhatsappApiEnabled(Boolean whatsappApiEnabled) { this.whatsappApiEnabled = whatsappApiEnabled; }
    public String getWhatsappPhoneNumberId() { return whatsappPhoneNumberId; }
    public void setWhatsappPhoneNumberId(String whatsappPhoneNumberId) { this.whatsappPhoneNumberId = whatsappPhoneNumberId; }
    public String getWhatsappBusinessAccountId() { return whatsappBusinessAccountId; }
    public void setWhatsappBusinessAccountId(String whatsappBusinessAccountId) { this.whatsappBusinessAccountId = whatsappBusinessAccountId; }
    public String getWhatsappAccessToken() { return whatsappAccessToken; }
    public void setWhatsappAccessToken(String whatsappAccessToken) { this.whatsappAccessToken = whatsappAccessToken; }
    public String getWhatsappApiVersion() { return whatsappApiVersion; }
    public void setWhatsappApiVersion(String whatsappApiVersion) { this.whatsappApiVersion = whatsappApiVersion; }
    public String getWhatsappVerifyToken() { return whatsappVerifyToken; }
    public void setWhatsappVerifyToken(String whatsappVerifyToken) { this.whatsappVerifyToken = whatsappVerifyToken; }

    // Getters and setters for aggregated entities
    public List<Host> getHosts() { return hosts; }
    public void setHosts(List<Host> hosts) { this.hosts = hosts; }
    public List<Guest> getGuests() { return guests; }
    public void setGuests(List<Guest> guests) { this.guests = guests; }
    public List<Invitation> getInvitations() { return invitations; }
    public void setInvitations(List<Invitation> invitations) { this.invitations = invitations; }
}
