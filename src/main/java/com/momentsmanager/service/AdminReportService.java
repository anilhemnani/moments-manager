package com.momentsmanager.service;

import com.momentsmanager.model.WeddingEvent;
import com.momentsmanager.repository.AttendeeRepository;
import com.momentsmanager.repository.EventTrafficLogRepository;
import com.momentsmanager.repository.GuestRepository;
import com.momentsmanager.repository.WeddingEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminReportService {

    @Autowired
    private WeddingEventRepository eventRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private AttendeeRepository attendeeRepository;

    @Autowired
    private EventTrafficLogRepository trafficRepository;

    public Map<String, Object> buildSummary() {
        Map<String, Object> summary = new HashMap<>();
        List<WeddingEvent> events = eventRepository.findAll();
        summary.put("weddingsCount", events.size());

        long guestsTotal = 0;
        long attendeesTotal = 0;
        Map<Long, Long> guestsPerEvent = new HashMap<>();
        Map<Long, Long> attendeesPerEvent = new HashMap<>();
        Map<Long, Long> trafficViews = new HashMap<>();
        Map<Long, LocalDateTime> lastUpdated = new HashMap<>();

        // Guests aggregated
        for (Object[] row : guestRepository.summarizeGuestsPerEvent()) {
            Long eventId = (Long) row[0];
            Long count = (Long) row[1];
            guestsPerEvent.put(eventId, count);
            guestsTotal += count;
        }

        // Attendees aggregated
        for (Object[] row : attendeeRepository.summarizeAttendeesPerEvent()) {
            Long eventId = (Long) row[0];
            Long count = (Long) row[1];
            attendeesPerEvent.put(eventId, count);
            attendeesTotal += count;
        }

        // Traffic aggregated
        for (Object[] row : trafficRepository.summarizeViewsPerEvent()) {
            Long eventId = (Long) row[0];
            Long views = (Long) row[1];
            trafficViews.put(eventId, views);
        }

        for (Object[] row : trafficRepository.summarizeLastViewPerEvent()) {
            Long eventId = (Long) row[0];
            LocalDateTime lastView = (LocalDateTime) row[1];
            lastUpdated.put(eventId, lastView);
        }

        // Last updated per event: prefer attendees updatedAt if available, else traffic last view
        for (WeddingEvent event : events) {
            var attendeeUpdated = attendeeRepository.findLastUpdatedByEventId(event.getId());
            var trafficUpdated = lastUpdated.get(event.getId());
            var effective = attendeeUpdated != null ? attendeeUpdated : trafficUpdated;
            lastUpdated.put(event.getId(), effective);
        }

        summary.put("guestsCount", guestsTotal);
        summary.put("attendeesCount", attendeesTotal);
        summary.put("guestsPerEvent", guestsPerEvent);
        summary.put("attendeesPerEvent", attendeesPerEvent);
        summary.put("trafficViews", trafficViews);
        summary.put("lastUpdated", lastUpdated);
        summary.put("generatedAt", LocalDateTime.now());
        return summary;
    }

    // Example filter/pagination method for attendees list (can be used in controller if you add a UI)
    public org.springframework.data.domain.Page<com.momentsmanager.model.Attendee> getAttendeesPage(Long eventId, int page, int size) {
        return attendeeRepository.findByRsvpGuestEventId(eventId, PageRequest.of(page, size));
    }

    public Page<WeddingEvent> findEventsPaged(String search, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return eventRepository.findByNameContainingIgnoreCase(search, pageable);
        }
        return eventRepository.findAll(pageable);
    }

    public Map<String, Object> buildSummaryForEvents(List<WeddingEvent> events) {
        Map<String, Object> summary = new HashMap<>();

        long guestsTotal = 0;
        long attendeesTotal = 0;
        Map<Long, Long> guestsPerEvent = new HashMap<>();
        Map<Long, Long> attendeesPerEvent = new HashMap<>();
        Map<Long, Long> trafficViews = new HashMap<>();
        Map<Long, LocalDateTime> lastUpdated = new HashMap<>();

        // Guests aggregated
        for (Object[] row : guestRepository.summarizeGuestsPerEvent()) {
            Long eventId = (Long) row[0];
            Long count = (Long) row[1];
            guestsPerEvent.put(eventId, count);
            guestsTotal += count;
        }

        // Attendees aggregated
        for (Object[] row : attendeeRepository.summarizeAttendeesPerEvent()) {
            Long eventId = (Long) row[0];
            Long count = (Long) row[1];
            attendeesPerEvent.put(eventId, count);
            attendeesTotal += count;
        }

        // Traffic aggregated
        for (Object[] row : trafficRepository.summarizeViewsPerEvent()) {
            Long eventId = (Long) row[0];
            Long views = (Long) row[1];
            trafficViews.put(eventId, views);
        }

        for (Object[] row : trafficRepository.summarizeLastViewPerEvent()) {
            Long eventId = (Long) row[0];
            LocalDateTime lastView = (LocalDateTime) row[1];
            lastUpdated.put(eventId, lastView);
        }

        // Last updated per event: prefer entity audit if present, else attendee updatedAt, else traffic last view
        for (WeddingEvent event : events) {
            LocalDateTime candidate = event.getUpdatedAt();
            if (candidate == null && attendeesPerEvent.containsKey(event.getId())) {
                var attendeeUpdated = attendeeRepository.findLastUpdatedByEventId(event.getId());
                candidate = attendeeUpdated != null ? attendeeUpdated : candidate;
            }
            if (candidate == null) {
                candidate = lastUpdated.get(event.getId());
            }
            lastUpdated.put(event.getId(), candidate);
        }

        summary.put("weddingsCount", events.size());
        summary.put("guestsCount", guestsTotal);
        summary.put("attendeesCount", attendeesTotal);
        summary.put("guestsPerEvent", guestsPerEvent);
        summary.put("attendeesPerEvent", attendeesPerEvent);
        summary.put("trafficViews", trafficViews);
        summary.put("lastUpdated", lastUpdated);
        summary.put("generatedAt", LocalDateTime.now());
        return summary;
    }
}
