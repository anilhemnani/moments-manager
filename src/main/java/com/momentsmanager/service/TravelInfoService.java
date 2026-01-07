package com.momentsmanager.service;

import com.momentsmanager.model.Guest;
import com.momentsmanager.model.TravelInfo;
import com.momentsmanager.model.WeddingEvent;
import com.momentsmanager.repository.GuestRepository;
import com.momentsmanager.repository.TravelInfoRepository;
import com.momentsmanager.repository.WeddingEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TravelInfoService {

    @Autowired
    private TravelInfoRepository travelInfoRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    @Transactional
    public TravelInfo createTravelInfo(Long guestId, TravelInfo travelInfo) {
        Optional<Guest> guestOpt = guestRepository.findById(guestId);
        if (guestOpt.isEmpty()) {
            throw new RuntimeException("Guest not found with id: " + guestId);
        }

        Guest guest = guestOpt.get();
        travelInfo.setGuest(guest);
        return travelInfoRepository.save(travelInfo);
    }

    @Transactional
    public TravelInfo updateTravelInfo(Long travelInfoId, TravelInfo updatedInfo) {
        Optional<TravelInfo> existingOpt = travelInfoRepository.findById(travelInfoId);
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("TravelInfo not found with id: " + travelInfoId);
        }

        TravelInfo existing = existingOpt.get();
        // Don't change the guest association - keep existing
        existing.setArrivalMode(updatedInfo.getArrivalMode());
        existing.setArrivalDateTime(updatedInfo.getArrivalDateTime());
        existing.setArrivalFlightNumber(updatedInfo.getArrivalFlightNumber());
        existing.setArrivalTrainNumber(updatedInfo.getArrivalTrainNumber());
        existing.setArrivalAirport(updatedInfo.getArrivalAirport());
        existing.setArrivalStation(updatedInfo.getArrivalStation());
        existing.setDepartureMode(updatedInfo.getDepartureMode());
        existing.setDepartureDateTime(updatedInfo.getDepartureDateTime());
        existing.setDepartureFlightNumber(updatedInfo.getDepartureFlightNumber());
        existing.setDepartureTrainNumber(updatedInfo.getDepartureTrainNumber());
        existing.setDepartureAirport(updatedInfo.getDepartureAirport());
        existing.setDepartureStation(updatedInfo.getDepartureStation());
        existing.setNeedsPickup(updatedInfo.getNeedsPickup());
        existing.setNeedsDrop(updatedInfo.getNeedsDrop());
        existing.setSpecialRequirements(updatedInfo.getSpecialRequirements());
        existing.setNotes(updatedInfo.getNotes());

        return travelInfoRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Optional<TravelInfo> getTravelInfoByGuestId(Long guestId) {
        return travelInfoRepository.findByGuestId(guestId);
    }

    @Transactional
    public void deleteTravelInfo(Long travelInfoId) {
        travelInfoRepository.deleteById(travelInfoId);
    }

    /**
     * Save or update travel information
     */
    @Transactional
    public TravelInfo saveTravelInfo(TravelInfo travelInfo) {
        if (travelInfo.getId() != null) {
            return updateTravelInfo(travelInfo.getId(), travelInfo);
        } else if (travelInfo.getGuest() != null) {
            return createTravelInfo(travelInfo.getGuest().getId(), travelInfo);
        } else {
            throw new RuntimeException("TravelInfo must have either an ID or a Guest");
        }
    }

    /**
     * Get all travel information for an event
     */
    @Transactional(readOnly = true)
    public List<TravelInfo> getTravelInfoByEvent(Long eventId) {
        WeddingEvent event = weddingEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        return travelInfoRepository.findAll().stream()
                .filter(ti -> ti.getGuest() != null
                        && ti.getGuest().getEvent() != null
                        && ti.getGuest().getEvent().getId().equals(eventId))
                .collect(Collectors.toList());
    }
}

