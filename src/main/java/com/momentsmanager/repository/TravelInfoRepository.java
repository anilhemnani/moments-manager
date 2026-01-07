package com.momentsmanager.repository;

import com.momentsmanager.model.TravelInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TravelInfoRepository extends JpaRepository<TravelInfo, Long> {
    Optional<TravelInfo> findByGuestId(Long guestId);
}

