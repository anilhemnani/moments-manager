package com.momentsmanager.web;

import com.momentsmanager.model.Attendee;
import com.momentsmanager.model.Guest;
import com.momentsmanager.model.RSVP;
import com.momentsmanager.model.TravelInfo;
import com.momentsmanager.model.WeddingEvent;
import com.momentsmanager.repository.AttendeeRepository;
import com.momentsmanager.repository.GuestRepository;
import com.momentsmanager.repository.RSVPRepository;
import com.momentsmanager.repository.TravelInfoRepository;
import com.momentsmanager.repository.WeddingEventRepository;
import com.momentsmanager.service.AttendeeService;
import com.momentsmanager.service.TravelInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/guests/{guestId}/rsvp/attendees")
public class AttendeeWebController {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private RSVPRepository rsvpRepository;

    @Autowired
    private AttendeeRepository attendeeRepository;

    @Autowired
    private AttendeeService attendeeService;

    @Autowired
    private TravelInfoRepository travelInfoRepository;

    @Autowired
    private TravelInfoService travelInfoService;

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST', 'GUEST')")
    @GetMapping
    public String listAttendees(@PathVariable Long guestId, Model model) {
        Optional<Guest> guestOpt = guestRepository.findById(guestId);
        if (guestOpt.isEmpty()) {
            return "redirect:/events";
        }

        Guest guest = guestOpt.get();
        Optional<RSVP> rsvpOpt = rsvpRepository.findByGuestId(guestId);
        if (rsvpOpt.isEmpty()) {
            return "redirect:/events/" + guest.getEventId() + "/guests";
        }

        RSVP rsvp = rsvpOpt.get();
        Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(guest.getEventId());
        if (eventOpt.isEmpty()) {
            return "redirect:/events";
        }

        List<Attendee> attendees = attendeeRepository.findByRsvpId(rsvp.getId());
        model.addAttribute("event", eventOpt.get());
        model.addAttribute("guest", guest);
        model.addAttribute("rsvp", rsvp);
        model.addAttribute("attendees", attendees);
        return "attendee_list";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST', 'GUEST')")
    @GetMapping("/new")
    public String newAttendee(@PathVariable Long guestId, Model model) {
        Optional<Guest> guestOpt = guestRepository.findById(guestId);
        if (guestOpt.isEmpty()) {
            return "redirect:/events";
        }

        Guest guest = guestOpt.get();
        Optional<RSVP> rsvpOpt = rsvpRepository.findByGuestId(guestId);
        if (rsvpOpt.isEmpty()) {
            return "redirect:/events/" + guest.getEventId() + "/guests";
        }

        RSVP rsvp = rsvpOpt.get();
        Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(guest.getEventId());
        if (eventOpt.isEmpty()) {
            return "redirect:/events";
        }

        model.addAttribute("event", eventOpt.get());
        model.addAttribute("guest", guest);
        model.addAttribute("rsvp", rsvp);
        model.addAttribute("attendee", new Attendee());
        return "attendee_form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST', 'GUEST')")
    @PostMapping("/new")
    public String createAttendee(@PathVariable Long guestId, @ModelAttribute Attendee attendee) {
        Optional<Guest> guestOpt = guestRepository.findById(guestId);
        if (guestOpt.isEmpty()) {
            return "redirect:/events";
        }

        Guest guest = guestOpt.get();
        Optional<RSVP> rsvpOpt = rsvpRepository.findByGuestId(guestId);
        if (rsvpOpt.isEmpty()) {
            return "redirect:/events/" + guest.getEventId() + "/guests";
        }

        attendeeService.createAttendee(rsvpOpt.get().getId(), attendee);
        return "redirect:/guests/" + guestId + "/rsvp/attendees";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST', 'GUEST')")
    @GetMapping("/{attendeeId}/edit")
    public String editAttendee(@PathVariable Long guestId, @PathVariable Long attendeeId, Model model) {
        Optional<Guest> guestOpt = guestRepository.findById(guestId);
        if (guestOpt.isEmpty()) {
            return "redirect:/events";
        }

        Guest guest = guestOpt.get();
        Optional<Attendee> attendeeOpt = attendeeRepository.findById(attendeeId);
        if (attendeeOpt.isEmpty()) {
            return "redirect:/guests/" + guestId + "/rsvp/attendees";
        }

        Attendee attendee = attendeeOpt.get();
        RSVP rsvp = attendee.getRsvp();
        Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(guest.getEventId());
        if (eventOpt.isEmpty()) {
            return "redirect:/events";
        }

        model.addAttribute("event", eventOpt.get());
        model.addAttribute("guest", guest);
        model.addAttribute("rsvp", rsvp);
        model.addAttribute("attendee", attendee);
        return "attendee_form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST', 'GUEST')")
    @PostMapping("/{attendeeId}/edit")
    public String updateAttendee(@PathVariable Long guestId, @PathVariable Long attendeeId, @ModelAttribute Attendee attendee) {
        attendeeService.updateAttendee(attendeeId, attendee);
        return "redirect:/guests/" + guestId + "/rsvp/attendees";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST', 'GUEST')")
    @PostMapping("/{attendeeId}/delete")
    public String deleteAttendee(@PathVariable Long guestId, @PathVariable Long attendeeId) {
        attendeeService.deleteAttendee(attendeeId);
        return "redirect:/guests/" + guestId + "/rsvp/attendees";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST', 'GUEST')")
    @GetMapping("/{attendeeId}/travel-info")
    public String manageTravelInfo(@PathVariable Long guestId, @PathVariable Long attendeeId, Model model) {
        Optional<Guest> guestOpt = guestRepository.findById(guestId);
        if (guestOpt.isEmpty()) {
            return "redirect:/events";
        }

        Guest guest = guestOpt.get();
        Optional<Attendee> attendeeOpt = attendeeRepository.findById(attendeeId);
        if (attendeeOpt.isEmpty()) {
            return "redirect:/guests/" + guestId + "/rsvp/attendees";
        }

        Attendee attendee = attendeeOpt.get();
        RSVP rsvp = attendee.getRsvp();
        Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(guest.getEventId());
        if (eventOpt.isEmpty()) {
            return "redirect:/events";
        }

        // Get existing travel info for guest or create new
        TravelInfo travelInfo = travelInfoRepository.findByGuestId(guestId)
                .orElseGet(() -> TravelInfo.builder().guest(guest).build());
        // Pre-populate defaults from event if available and fields are null
        eventOpt.ifPresent(ev -> {
            if (travelInfo.getGuest() == null) {
                travelInfo.setGuest(guest);
            }
            if (travelInfo.getArrivalAirport() == null) {
                travelInfo.setArrivalAirport(ev.getPreferredTravelAirport());
            }
            if (travelInfo.getArrivalStation() == null) {
                travelInfo.setArrivalStation(ev.getPreferredTravelStation());
            }
            if (travelInfo.getDepartureAirport() == null) {
                travelInfo.setDepartureAirport(ev.getPreferredTravelAirport());
            }
            if (travelInfo.getDepartureStation() == null) {
                travelInfo.setDepartureStation(ev.getPreferredTravelStation());
            }
            if (travelInfo.getArrivalDateTime() == null && ev.getExpectedGuestArrivalDate() != null) {
                travelInfo.setArrivalDateTime(ev.getExpectedGuestArrivalDate().atStartOfDay());
            }
            if (travelInfo.getDepartureDateTime() == null && ev.getExpectedGuestDepartureDate() != null) {
                travelInfo.setDepartureDateTime(ev.getExpectedGuestDepartureDate().atStartOfDay());
            }
        });

        model.addAttribute("event", eventOpt.get());
        model.addAttribute("guest", guest);
        model.addAttribute("rsvp", rsvp);
        model.addAttribute("attendee", attendee);
        model.addAttribute("travelInfo", travelInfo);
        return "travel_info_form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST', 'GUEST')")
    @PostMapping("/{attendeeId}/travel-info")
    public String saveTravelInfo(@PathVariable Long guestId, @PathVariable Long attendeeId, @ModelAttribute TravelInfo travelInfo) {
        // TODO: Travel Info is now at Guest level - this method should be removed or refactored
        // Optional<TravelInfo> existingOpt = travelInfoRepository.findByAttendeeId(attendeeId);
        // if (existingOpt.isPresent()) {
        //     travelInfoService.updateTravelInfo(existingOpt.get().getId(), travelInfo);
        // } else {
        //     travelInfoService.createTravelInfo(attendeeId, travelInfo);
        // }
        return "redirect:/guests/" + guestId + "/rsvp/attendees";
    }
}
