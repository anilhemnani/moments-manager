package com.momentsmanager.repository;

import com.momentsmanager.model.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    Optional<Guest> findByFamilyNameIgnoreCaseAndContactPhone(String familyName, String contactPhone);
    Guest findByContactPhone(String contactPhone);

    @Query("select g.event.id as eventId, count(g) as guests from Guest g group by g.event.id")
    List<Object[]> summarizeGuestsPerEvent();
}
