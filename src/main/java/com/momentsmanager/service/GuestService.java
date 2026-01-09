package com.momentsmanager.service;

import com.momentsmanager.model.Guest;
import com.momentsmanager.model.RSVP;
import com.momentsmanager.model.RSVPStatus;
import com.momentsmanager.repository.GuestRepository;
import com.momentsmanager.repository.RSVPRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GuestService {

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private RSVPRepository rsvpRepository;

    @Transactional
    public Guest createGuest(Guest guest) {
        // Save the guest first
        Guest savedGuest = guestRepository.save(guest);

        // Create and save RSVP with default "Pending" status
        RSVP rsvp = RSVP.builder()
                .guest(savedGuest)
                .eventId(savedGuest.getEventId())
                .status(RSVPStatus.PENDING)
                .attendeeCount(0)
                .build();

        rsvpRepository.save(rsvp);

        return savedGuest;
    }

    @Transactional
    public Guest updateGuest(Long id, Guest guestDetails) {
        Optional<Guest> guestOpt = guestRepository.findById(id);
        if (guestOpt.isPresent()) {
            Guest guest = guestOpt.get();
            guest.setFamilyName(guestDetails.getFamilyName());
            guest.setContactName(guestDetails.getContactName());
            guest.setContactEmail(guestDetails.getContactEmail());
            guest.setContactPhone(guestDetails.getContactPhone());
            guest.setSide(guestDetails.getSide());
            guest.setAddress(guestDetails.getAddress());
            guest.setMaxAttendees(guestDetails.getMaxAttendees());
            guest.setEventId(guestDetails.getEventId());
            return guestRepository.save(guest);
        }
        throw new RuntimeException("Guest not found with id: " + id);
    }

    public List<Guest> getAllGuests() {
        return guestRepository.findAll();
    }

    public Optional<Guest> getGuestById(Long id) {
        return guestRepository.findById(id);
    }

    public List<Guest> getGuestsByEventId(Long eventId) {
        return guestRepository.findAll().stream()
                .filter(guest -> guest.getEventId().equals(eventId))
                .toList();
    }

    @Transactional
    public void deleteGuest(Long id) {
        guestRepository.deleteById(id);
    }

    public Optional<RSVP> getGuestRSVP(Long guestId) {
        return rsvpRepository.findByGuestId(guestId);
    }

    @Transactional
    public RSVP updateGuestRSVP(Long guestId, RSVPStatus status, int attendeeCount) {
        Optional<RSVP> rsvpOpt = rsvpRepository.findByGuestId(guestId);
        if (rsvpOpt.isPresent()) {
            RSVP rsvp = rsvpOpt.get();
            rsvp.setStatus(status);
            rsvp.setAttendeeCount(attendeeCount);
            return rsvpRepository.save(rsvp);
        }
        throw new RuntimeException("RSVP not found for guest id: " + guestId);
    }
}

