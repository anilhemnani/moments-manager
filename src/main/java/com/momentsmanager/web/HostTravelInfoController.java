package com.momentsmanager.web;

import com.momentsmanager.model.Guest;
import com.momentsmanager.model.TravelInfo;
import com.momentsmanager.model.WeddingEvent;
import com.momentsmanager.repository.GuestRepository;
import com.momentsmanager.repository.WeddingEventRepository;
import com.momentsmanager.service.TravelInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for hosts to manage guest travel information
 */
@Controller
@RequestMapping("/host")
public class HostTravelInfoController {

    @Autowired
    private TravelInfoService travelInfoService;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    /**
     * View all travel information for an event
     */
    @PreAuthorize("hasRole('HOST')")
    @GetMapping("/travel-info")
    public String viewTravelInfo(
            @RequestParam(required = false) Long eventId,
            Authentication authentication,
            Model model) {

        // Get host's events
        String hostEmail = authentication.getName();
        List<WeddingEvent> hostEvents = weddingEventRepository.findByHostEmail(hostEmail);

        if (hostEvents.isEmpty()) {
            model.addAttribute("error", "No events found for your account");
            model.addAttribute("events", hostEvents);
            return "host/travel_info_list";
        }

        // If no event specified, use the first one
        if (eventId == null && !hostEvents.isEmpty()) {
            eventId = hostEvents.get(0).getId();
        }

        model.addAttribute("events", hostEvents);
        model.addAttribute("selectedEventId", eventId);

        if (eventId != null) {
            // Get all travel info for the event
            List<TravelInfo> travelInfoList = travelInfoService.getTravelInfoByEvent(eventId);
            model.addAttribute("travelInfoList", travelInfoList);
        }

        return "host/travel_info_list";
    }

    /**
     * Show form to add/edit travel information for a guest
     */
    @PreAuthorize("hasRole('HOST')")
    @GetMapping("/travel-info/edit")
    public String editTravelInfoForm(
            @RequestParam Long guestId,
            @RequestParam Long eventId,
            Model model) {

        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new RuntimeException("Guest not found"));

        WeddingEvent event = weddingEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        TravelInfo travelInfo = guest.getTravelInfo();
        if (travelInfo == null) {
            travelInfo = new TravelInfo();
            travelInfo.setGuest(guest);
        }

        // Pre-populate defaults from event if missing
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

        model.addAttribute("travelInfo", travelInfo);
        model.addAttribute("guest", guest);
        model.addAttribute("event", event);
        model.addAttribute("eventId", eventId);

        return "host/travel_info_form";
    }

    /**
     * Save travel information
     */
    @PreAuthorize("hasRole('HOST')")
    @PostMapping("/travel-info/save")
    public String saveTravelInfo(
            @ModelAttribute TravelInfo travelInfo,
            @RequestParam Long guestId,
            @RequestParam Long eventId) {

        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new RuntimeException("Guest not found"));
        WeddingEvent event = weddingEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        travelInfo.setGuest(guest);

        // Apply event defaults when missing
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

        travelInfoService.saveTravelInfo(travelInfo);

        return "redirect:/host/travel-info?eventId=" + eventId;
    }

    /**
     * Delete travel information
     */
    @PreAuthorize("hasRole('HOST')")
    @PostMapping("/travel-info/{travelInfoId}/delete")
    public String deleteTravelInfo(
            @PathVariable Long travelInfoId,
            @RequestParam Long eventId) {

        travelInfoService.deleteTravelInfo(travelInfoId);
        return "redirect:/host/travel-info?eventId=" + eventId;
    }
}
