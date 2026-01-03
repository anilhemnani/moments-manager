package com.momentsmanager.repository;

import com.momentsmanager.model.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    Optional<Guest> findByFamilyNameIgnoreCaseAndContactPhone(String familyName, String contactPhone);
    Guest findByContactPhone(String contactPhone);
    // ...existing code...
}
