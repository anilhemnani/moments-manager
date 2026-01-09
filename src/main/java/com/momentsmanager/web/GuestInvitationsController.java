package com.momentsmanager.web;

import com.momentsmanager.model.*;
import com.momentsmanager.repository.InvitationLogRepository;
import com.momentsmanager.repository.GuestRepository;
import com.momentsmanager.repository.RSVPRepository;
import com.momentsmanager.repository.TravelInfoRepository;
import com.momentsmanager.repository.WeddingEventRepository;
import com.momentsmanager.repository.AttendeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for guest invitation management
 * Guests can view invitations, RSVP, add attendees, and manage travel details
 */
@Controller
@RequestMapping("/invitations")
public class GuestInvitationsController {

    @Autowired
    private InvitationLogRepository invitationLogRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private RSVPRepository rsvpRepository;

    @Autowired
    private TravelInfoRepository travelInfoRepository;

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    @Autowired
    private AttendeeRepository attendeeRepository;

    /**
     * Guest invitations list - shows all invitations for the guest
     * If only one invitation exists, redirects directly to it
     */
    @PreAuthorize("hasRole('GUEST')")
    @GetMapping
    public String listInvitations(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return "redirect:/login";
        }

        String principal = auth.getName();
        String guestPhoneNumber = extractPhoneNumber(principal);

        List<Invitation> guestInvitations = invitationLogRepository
                .findByGuestPhoneNumber(guestPhoneNumber)
                .stream()
                .map(log -> log.getInvitation())
                .distinct()
                .toList();

        if (guestInvitations.size() == 1) {
            return "redirect:/invitations/" + guestInvitations.getFirst().getId();
        }

        if (guestInvitations.isEmpty()) {
            model.addAttribute("emptyState", true);
            return "guest_invitations";
        }

        model.addAttribute("invitations", guestInvitations);
        return "guest_invitations";
    }

    /**
     * View specific invitation for guest
     */
    @PreAuthorize("hasRole('GUEST')")
    @GetMapping("/{invitationId}")
    public String viewInvitation(@PathVariable Long invitationId, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return "redirect:/login";
        }

        String principal = auth.getName();
        String[] parts = extractFamilyNameAndPhone(principal);
        String familyName = parts[0];
        String guestPhoneNumber = parts[1];

        var invitationLog = invitationLogRepository
                .findByInvitationIdAndGuestPhoneNumber(invitationId, guestPhoneNumber)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        Invitation invitation = invitationLog.getInvitation();
        
        Optional<Guest> guestOpt = guestRepository.findByFamilyNameIgnoreCaseAndContactPhone(familyName, guestPhoneNumber);
        if (guestOpt.isEmpty()) {
            throw new RuntimeException("Guest not found");
        }
        
        Guest guest = guestOpt.get();
        Optional<RSVP> rsvpOpt = rsvpRepository.findByGuestId(guest.getId());
        
        model.addAttribute("invitation", invitation);
        model.addAttribute("event", invitation.getEvent());
        model.addAttribute("invitationLog", invitationLog);
        model.addAttribute("guest", guest);
        model.addAttribute("rsvp", rsvpOpt.orElse(null));

        return "guest_invitation_view";
    }

    /**
     * Guest RSVP form - now separate from attendees
     */
    @PreAuthorize("hasRole('GUEST')")
    @GetMapping("/rsvp/form")
    public String rsvpForm(@RequestParam Long guestId, @RequestParam Long eventId, Model model) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new RuntimeException("Guest not found"));

        // Verify the guest belongs to the authenticated user
        verifyGuestAccess(guest);

        WeddingEvent event = weddingEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        Optional<RSVP> rsvpOpt = rsvpRepository.findByGuestId(guest.getId());
        
        RSVP rsvp = rsvpOpt.orElseGet(() -> RSVP.builder()
                .guest(guest)
                .eventId(eventId)
                .status(RSVPStatus.PENDING)
                .attendeeCount(1)
                .build());
        
        model.addAttribute("guest", guest);
        model.addAttribute("event", event);
        model.addAttribute("rsvp", rsvp);
        model.addAttribute("eventId", eventId);

        return "guest_rsvp_attendees_form_new";
    }

    /**
     * Save RSVP status and attendee count only
     */
    @PreAuthorize("hasRole('GUEST')")
    @PostMapping("/rsvp/save")
    public String saveRSVP(
            @RequestParam Long guestId,
            @RequestParam Long eventId,
            @RequestParam RSVPStatus status,
            @RequestParam(required = false) Integer attendeeCount,
            RedirectAttributes redirectAttributes) {

        try {
            Guest guest = guestRepository.findById(guestId)
                    .orElseThrow(() -> new RuntimeException("Guest not found"));

            // Verify the guest belongs to the authenticated user
            verifyGuestAccess(guest);

            WeddingEvent event = weddingEventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            // Create or update RSVP
            Optional<RSVP> rsvpOpt = rsvpRepository.findByGuestId(guest.getId());
            RSVP rsvp = rsvpOpt.orElseGet(() -> RSVP.builder()
                    .guest(guest)
                    .eventId(eventId)
                    .build());

            rsvp.setStatus(status);

            // If ACCEPTED, attendee count is required
            if (status == RSVPStatus.ACCEPTED) {
                if (attendeeCount == null || attendeeCount < 1) {
                    throw new RuntimeException("Number of attendees is required when accepting the invitation");
                }
                if (attendeeCount > guest.getMaxAttendees()) {
                    throw new RuntimeException("Number of attendees cannot exceed " + guest.getMaxAttendees());
                }
                rsvp.setAttendeeCount(attendeeCount);
            } else {
                // For other statuses, set to 0 or 1
                rsvp.setAttendeeCount(attendeeCount != null ? attendeeCount : 0);
            }

            rsvpRepository.save(rsvp);

            redirectAttributes.addFlashAttribute("successMessage",
                    "RSVP saved successfully! You can now update your travel details and attendee information.");
            return "redirect:/invitations";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error saving RSVP: " + e.getMessage());
            return "redirect:/invitations/rsvp/form?guestId=" + guestId + "&eventId=" + eventId;
        }
    }

    /**
     * Merged RSVP and Attendees form endpoint (DEPRECATED - kept for compatibility)
     */
    @PreAuthorize("hasRole('GUEST')")
    @PostMapping("/rsvp-attendees/save")
    public String saveRSVPAndAttendees(
            @RequestParam Long guestId,
            @RequestParam Long eventId,
            @RequestParam RSVPStatus status,
            @RequestParam int attendeeCount,
            @RequestParam(required = false) List<String> attendeeNames,
            @RequestParam(required = false) List<String> attendeeAges,
            RedirectAttributes redirectAttributes) {

        try {
            Guest guest = guestRepository.findById(guestId)
                    .orElseThrow(() -> new RuntimeException("Guest not found"));

            // Verify the guest belongs to the authenticated user
            verifyGuestAccess(guest);

            WeddingEvent event = weddingEventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            // Validate attendee count
            if (attendeeCount < 1 || attendeeCount > guest.getMaxAttendees()) {
                throw new RuntimeException("Invalid attendee count. Maximum allowed: " + guest.getMaxAttendees());
            }

            // Get or create RSVP
            Optional<RSVP> rsvpOpt = rsvpRepository.findByGuestId(guest.getId());
            RSVP rsvp = rsvpOpt.orElseGet(() -> RSVP.builder()
                    .guest(guest)
                    .eventId(eventId)
                    .build());

            // Update RSVP
            rsvp.setStatus(status);
            rsvp.setAttendeeCount(attendeeCount);
            rsvp = rsvpRepository.save(rsvp);

            // Clear existing attendees
            if (rsvp.getAttendees() != null) {
                attendeeRepository.deleteAll(rsvp.getAttendees());
            }

            // Create new attendees from form data
            if (attendeeNames != null && !attendeeNames.isEmpty()) {
                for (int i = 0; i < attendeeNames.size() && i < attendeeCount; i++) {
                    String name = attendeeNames.get(i);
                    String ageGroup = (attendeeAges != null && i < attendeeAges.size())
                            ? attendeeAges.get(i)
                            : "Adult";

                    if (name != null && !name.trim().isEmpty()) {
                        Attendee attendee = Attendee.builder()
                                .rsvp(rsvp)
                                .name(name.trim())
                                .ageGroup(ageGroup)
                                .build();
                        attendeeRepository.save(attendee);
                    }
                }
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "RSVP and attendees saved successfully!");
            return "redirect:/invitations";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to save RSVP and attendees: " + e.getMessage());
            return "redirect:/invitations/rsvp/form?guestId=" + guestId + "&eventId=" + eventId;
        }
    }

    /**
     * Update RSVP from guest
     */
    @PreAuthorize("hasRole('GUEST')")
    @PostMapping("/rsvp/update")
    public String updateRSVP(
            @RequestParam RSVPStatus status,
            @RequestParam int attendeeCount,
            RedirectAttributes redirectAttributes) {
        
        Guest guest = getAuthenticatedGuest();
        Optional<RSVP> rsvpOpt = rsvpRepository.findByGuestId(guest.getId());
        
        RSVP rsvp = rsvpOpt.orElseGet(() -> RSVP.builder()
                .guest(guest)
                .eventId(guest.getEventId())
                .build());
        
        rsvp.setStatus(status);
        rsvp.setAttendeeCount(attendeeCount);
        rsvpRepository.save(rsvp);
        
        redirectAttributes.addFlashAttribute("successMessage", 
                "RSVP updated successfully to: " + status);
        return "redirect:/invitations";
    }

    /**
     * Attendees management for guest
     */
    @PreAuthorize("hasRole('GUEST')")
    @GetMapping("/attendees")
    public String attendeesList(Model model) {
        Guest guest = getAuthenticatedGuest();
        Optional<RSVP> rsvpOpt = rsvpRepository.findByGuestId(guest.getId());
        
        if (rsvpOpt.isEmpty()) {
            return "redirect:/invitations/rsvp/form";
        }
        
        RSVP rsvp = rsvpOpt.get();
        model.addAttribute("guest", guest);
        model.addAttribute("rsvp", rsvp);
        model.addAttribute("attendees", rsvp.getAttendees());
        return "guest_attendees_form";
    }

    /**
     * Create or update an attendee for the authenticated guest's RSVP
     */
    @PreAuthorize("hasRole('GUEST')")
    @PostMapping("/attendees")
    public String saveAttendee(@RequestParam(required = false) Long attendeeId,
                               @RequestParam String name,
                               @RequestParam String ageGroup,
                               @RequestParam(required = false) String mobileNumber,
                               RedirectAttributes redirectAttributes) {

        Guest guest = getAuthenticatedGuest();
        RSVP rsvp = rsvpRepository.findByGuestId(guest.getId())
                .orElseThrow(() -> new RuntimeException("RSVP not found for guest"));

        Attendee attendee;
        if (attendeeId != null) {
            attendee = attendeeRepository.findById(attendeeId)
                    .orElseThrow(() -> new RuntimeException("Attendee not found"));
            if (!attendee.getRsvp().getGuest().getId().equals(guest.getId())) {
                throw new RuntimeException("Access denied: cannot modify another guest's attendee");
            }
            attendee.setName(name);
            attendee.setAgeGroup(ageGroup);
            attendee.setMobileNumber(mobileNumber);
        } else {
            attendee = new Attendee();
            attendee.setRsvp(rsvp);
            attendee.setName(name);
            attendee.setAgeGroup(ageGroup);
            attendee.setMobileNumber(mobileNumber);
        }

        attendeeRepository.save(attendee);
        redirectAttributes.addFlashAttribute("successMessage", "Attendee saved successfully");
        return "redirect:/invitations/attendees";
    }

    /**
     * Delete an attendee for the authenticated guest's RSVP
     */
    @PreAuthorize("hasRole('GUEST')")
    @PostMapping("/attendees/{attendeeId}/delete")
    public String deleteAttendee(@PathVariable Long attendeeId, RedirectAttributes redirectAttributes) {
        Guest guest = getAuthenticatedGuest();
        Attendee attendee = attendeeRepository.findById(attendeeId)
                .orElseThrow(() -> new RuntimeException("Attendee not found"));

        if (!attendee.getRsvp().getGuest().getId().equals(guest.getId())) {
            throw new RuntimeException("Access denied: cannot delete another guest's attendee");
        }

        attendeeRepository.delete(attendee);
        redirectAttributes.addFlashAttribute("successMessage", "Attendee deleted successfully");
        return "redirect:/invitations/attendees";
    }

    /**
     * Travel information for guest
     */
    @PreAuthorize("hasRole('GUEST')")
    @GetMapping("/travel-info")
    public String travelInfoForm(@RequestParam Long guestId, @RequestParam Long eventId, Model model) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new RuntimeException("Guest not found"));

        // Verify the guest belongs to the authenticated user
        verifyGuestAccess(guest);

        WeddingEvent event = weddingEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        Optional<TravelInfo> travelInfoOpt = travelInfoRepository.findByGuestId(guest.getId());

        TravelInfo travelInfo = travelInfoOpt.orElseGet(() -> TravelInfo.builder()
                .guest(guest)
                .build());

        // Pre-populate defaults from event when empty
        if (travelInfo.getGuest() == null) {
            travelInfo.setGuest(guest);
        }
        if (travelInfo.getArrivalAirport() == null) {
            travelInfo.setArrivalAirport(event.getPreferredTravelAirport());
        }
        if (travelInfo.getArrivalStation() == null) {
            travelInfo.setArrivalStation(event.getPreferredTravelStation());
        }
        if (travelInfo.getDepartureAirport() == null) {
            travelInfo.setDepartureAirport(event.getPreferredTravelAirport());
        }
        if (travelInfo.getDepartureStation() == null) {
            travelInfo.setDepartureStation(event.getPreferredTravelStation());
        }
        if (travelInfo.getArrivalDateTime() == null && event.getExpectedGuestArrivalDate() != null) {
            travelInfo.setArrivalDateTime(event.getExpectedGuestArrivalDate().atStartOfDay());
        }
        if (travelInfo.getDepartureDateTime() == null && event.getExpectedGuestDepartureDate() != null) {
            travelInfo.setDepartureDateTime(event.getExpectedGuestDepartureDate().atStartOfDay());
        }

        // Get RSVP information
        Optional<RSVP> rsvpOpt = rsvpRepository.findByGuestId(guest.getId());
        RSVP rsvp = rsvpOpt.orElseGet(() -> RSVP.builder()
                .guest(guest)
                .eventId(eventId)
                .attendeeCount(1)
                .build());

        // Get existing attendees
        List<Attendee> attendees = new ArrayList<>();
        if (rsvp.getAttendees() != null) {
            attendees = new ArrayList<>(rsvp.getAttendees());
        }

        model.addAttribute("guest", guest);
        model.addAttribute("event", event);
        model.addAttribute("travelInfo", travelInfo);
        model.addAttribute("rsvp", rsvp);
        model.addAttribute("attendees", attendees);
        model.addAttribute("eventId", eventId);
        return "guest_travel_info_form";
    }

    /**
     * Update travel information from guest
     */
    @PreAuthorize("hasRole('GUEST')")
    @PostMapping("/travel-info/save")
    public String saveTravelInfo(
            @RequestParam Long guestId,
            @RequestParam Long eventId,
            @ModelAttribute TravelInfo travelInfo,
            @RequestParam(required = false) List<String> attendeeNames,
            @RequestParam(required = false) List<String> attendeeAges,
            RedirectAttributes redirectAttributes) {

        try {
            Guest guest = guestRepository.findById(guestId)
                    .orElseThrow(() -> new RuntimeException("Guest not found"));

            // Verify the guest belongs to the authenticated user
            verifyGuestAccess(guest);

            WeddingEvent event = weddingEventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            // Save travel information
            travelInfo.setGuest(guest);
            if (travelInfo.getArrivalAirport() == null) {
                travelInfo.setArrivalAirport(event.getPreferredTravelAirport());
            }
            if (travelInfo.getArrivalStation() == null) {
                travelInfo.setArrivalStation(event.getPreferredTravelStation());
            }
            if (travelInfo.getDepartureAirport() == null) {
                travelInfo.setDepartureAirport(event.getPreferredTravelAirport());
            }
            if (travelInfo.getDepartureStation() == null) {
                travelInfo.setDepartureStation(event.getPreferredTravelStation());
            }
            if (travelInfo.getArrivalDateTime() == null && event.getExpectedGuestArrivalDate() != null) {
                travelInfo.setArrivalDateTime(event.getExpectedGuestArrivalDate().atStartOfDay());
            }
            if (travelInfo.getDepartureDateTime() == null && event.getExpectedGuestDepartureDate() != null) {
                travelInfo.setDepartureDateTime(event.getExpectedGuestDepartureDate().atStartOfDay());
            }
            travelInfoRepository.save(travelInfo);

            // Save attendees if provided
            if (attendeeNames != null && !attendeeNames.isEmpty()) {
                // Get RSVP to associate attendees
                Optional<RSVP> rsvpOpt = rsvpRepository.findByGuestId(guest.getId());
                if (rsvpOpt.isPresent()) {
                    RSVP rsvp = rsvpOpt.get();

                    // Delete existing attendees
                    if (rsvp.getAttendees() != null) {
                        attendeeRepository.deleteAll(rsvp.getAttendees());
                    }

                    // Create new attendees
                    for (int i = 0; i < attendeeNames.size(); i++) {
                        String name = attendeeNames.get(i);
                        String ageGroup = (attendeeAges != null && i < attendeeAges.size())
                                ? attendeeAges.get(i)
                                : "Adult";

                        if (name != null && !name.trim().isEmpty()) {
                            Attendee attendee = Attendee.builder()
                                    .rsvp(rsvp)
                                    .name(name.trim())
                                    .ageGroup(ageGroup)
                                    .build();
                            attendeeRepository.save(attendee);
                        }
                    }
                }
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Travel information and attendee details saved successfully");
            return "redirect:/invitations";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error saving travel information: " + e.getMessage());
            return "redirect:/invitations/travel-info?guestId=" + guestId + "&eventId=" + eventId;
        }
    }

    /**
     * Verify that the authenticated guest has access to the specified guest record
     */
    private void verifyGuestAccess(Guest guest) {
        Guest authenticatedGuest = getAuthenticatedGuest();
        if (!authenticatedGuest.getId().equals(guest.getId())) {
            throw new RuntimeException("Access denied: You can only access your own information");
        }
    }

    /**
     * Get authenticated guest from session
     */
    private Guest getAuthenticatedGuest() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Guest not authenticated");
        }
        
        String principal = auth.getName();
        String[] parts = extractFamilyNameAndPhone(principal);
        String familyName = parts[0];
        String guestPhoneNumber = parts[1];

        Optional<Guest> guestOpt = guestRepository.findByFamilyNameIgnoreCaseAndContactPhone(familyName, guestPhoneNumber);
        if (guestOpt.isEmpty()) {
            throw new RuntimeException("Guest not found");
        }
        return guestOpt.get();
    }

    /**
     * Extract family name and phone from principal (format: "FamilyName_PhoneNumber")
     * Returns [familyName, phoneNumber]
     */
    private String[] extractFamilyNameAndPhone(String principal) {
        if (principal == null || !principal.contains("_")) {
            return new String[]{principal, principal};
        }
        // Split by underscore - everything before last underscore is family name
        int lastUnderscore = principal.lastIndexOf("_");
        String familyName = principal.substring(0, lastUnderscore);
        String phoneNumber = principal.substring(lastUnderscore + 1);
        return new String[]{familyName, phoneNumber};
    }

    /**
     * Extract phone number from principal (format: "FamilyName_PhoneNumber")
     */
    private String extractPhoneNumber(String principal) {
        if (principal == null || !principal.contains("_")) {
            return principal;
        }
        String[] parts = principal.split("_");
        return parts[parts.length - 1];
    }
}
