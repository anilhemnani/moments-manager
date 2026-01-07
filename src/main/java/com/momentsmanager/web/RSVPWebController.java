package com.momentsmanager.web;

import com.momentsmanager.model.RSVP;
import com.momentsmanager.model.Guest;
import com.momentsmanager.model.WeddingEvent;
import com.momentsmanager.repository.RSVPRepository;
import com.momentsmanager.repository.GuestRepository;
import com.momentsmanager.repository.WeddingEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST', 'GUEST')")
    @GetMapping
    public String viewRSVP(@PathVariable Long guestId, Model model) {
        Optional<Guest> guestOpt = guestRepository.findById(guestId);
        if (guestOpt.isEmpty()) {
            return "redirect:/events";
        }

        Guest guest = guestOpt.get();
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
    @PreAuthorize("hasRole('HOST')")
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
        Optional<RSVP> rsvpOpt = rsvpRepository.findByGuestId(guestId);
        if (rsvpOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "RSVP not found for this guest");
            return "redirect:/guests/" + guestId + "/rsvp";
        }

        // Validate status
        if (!isValidRSVPStatus(status)) {
            redirectAttributes.addFlashAttribute("error", "Invalid RSVP status");
            return "redirect:/guests/" + guestId + "/rsvp";
        }

        RSVP rsvp = rsvpOpt.get();
        String oldStatus = rsvp.getStatus();
        rsvp.setStatus(status);
        rsvpRepository.save(rsvp);

        redirectAttributes.addFlashAttribute("successMessage",
                "RSVP status updated from '" + oldStatus + "' to '" + status + "'");
        return "redirect:/guests/" + guestId + "/rsvp";
    }

    /**
     * Validate RSVP status values
     */
    private boolean isValidRSVPStatus(String status) {
        return status != null && (
                status.equals("Pending") ||
                status.equals("Accepted") ||
                status.equals("Declined") ||
                status.equals("Maybe")
        );
    }
}

