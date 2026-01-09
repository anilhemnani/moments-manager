package com.momentsmanager.web;

import com.momentsmanager.model.RSVP;
import com.momentsmanager.model.RSVPStatus;
import com.momentsmanager.model.Guest;
import com.momentsmanager.model.WeddingEvent;
import com.momentsmanager.repository.RSVPRepository;
import com.momentsmanager.repository.GuestRepository;
import com.momentsmanager.repository.WeddingEventRepository;
import com.momentsmanager.service.AccessAuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Controller
@RequestMapping("/guests/{guestId}/rsvp")
public class RSVPWebController {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private RSVPRepository rsvpRepository;

    @Autowired
    private AccessAuditService accessAuditService;

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST', 'GUEST')")
    @GetMapping
    public String viewRSVP(@PathVariable Long guestId, Model model, Authentication authentication, HttpServletRequest request) {
        Optional<Guest> guestOpt = guestRepository.findById(guestId);
        if (guestOpt.isEmpty()) {
            return "redirect:/events";
        }

        Guest guest = guestOpt.get();

        // Enforce that GUEST role may only access their own RSVP (no URL tampering)
        if (authentication != null && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_GUEST".equals(a.getAuthority()))) {
            String username = authentication.getName();
            String guestLogin = guest.getContactEmail();
            if (guestLogin == null || username == null || !guestLogin.equalsIgnoreCase(username)) {
                accessAuditService.logUnauthorized(request, authentication, "Guest tried to access RSVP for guestId=" + guestId);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: you can only view your own RSVP");
            }
        }

        Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(guest.getEventId());
        if (eventOpt.isEmpty()) {
            return "redirect:/events";
        }

        Optional<RSVP> rsvpOpt = rsvpRepository.findByGuestId(guestId);
        if (rsvpOpt.isEmpty()) {
            return "redirect:/events/" + guest.getEventId() + "/guests";
        }

        model.addAttribute("event", eventOpt.get());
        model.addAttribute("guest", guest);
        model.addAttribute("rsvp", rsvpOpt.get());
        return "rsvp_view";
    }

    /**
     * Host updates RSVP status for a guest
     */
    @PreAuthorize("hasRole('HOST') or hasRole('ADMIN')")
    @PostMapping("/update-status")
    public String updateRSVPStatus(
            @PathVariable Long guestId,
            @RequestParam String status,
            @RequestParam Long eventId,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        Optional<Guest> guestOpt = guestRepository.findById(guestId);
        if (guestOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Guest not found");
            return "redirect:/host/dashboard";
        }

        Guest guest = guestOpt.get();

        // Prevent a host from updating RSVP of a guest not in their event (URL tampering)
        if (authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOST"))) {
            if (!guest.getEventId().equals(eventId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: guest does not belong to this event");
            }
        }

        Optional<RSVP> rsvpOpt = rsvpRepository.findByGuestId(guestId);
        if (rsvpOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "RSVP not found for this guest");
            return "redirect:/guests/" + guestId + "/rsvp";
        }

        // Validate status
        try {
            RSVPStatus rsvpStatus = RSVPStatus.valueOf(status.toUpperCase());
            RSVP rsvp = rsvpOpt.get();
            RSVPStatus oldStatus = rsvp.getStatus();
            rsvp.setStatus(rsvpStatus);
            rsvpRepository.save(rsvp);

            redirectAttributes.addFlashAttribute("successMessage",
                    "RSVP status updated from '" + oldStatus + "' to '" + rsvpStatus + "'");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid RSVP status");
        }
        return "redirect:/guests/" + guestId + "/rsvp";
    }

    /**
     * Validate RSVP status values
     */
    private boolean isValidRSVPStatus(String status) {
        try {
            RSVPStatus.valueOf(status.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
