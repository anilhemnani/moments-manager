package com.momentsmanager.repository;

import com.momentsmanager.model.Attendee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendeeRepository extends JpaRepository<Attendee, Long> {
    @Query("select a.rsvp.guest.event.id as eventId, count(a) as attendees from Attendee a group by a.rsvp.guest.event.id")
    List<Object[]> summarizeAttendeesPerEvent();

    @Query("select max(a.updatedAt) from Attendee a where a.rsvp.guest.event.id = :eventId")
    LocalDateTime findLastUpdatedByEventId(Long eventId);

    Page<Attendee> findByRsvpGuestEventId(Long eventId, Pageable pageable);

    List<Attendee> findByRsvpId(Long rsvpId);
}
