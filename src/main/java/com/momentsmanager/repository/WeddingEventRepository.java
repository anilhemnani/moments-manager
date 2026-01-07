package com.momentsmanager.repository;

import com.momentsmanager.model.WeddingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WeddingEventRepository extends JpaRepository<WeddingEvent, Long> {
    @Query("SELECT DISTINCT h.event FROM Host h WHERE h.email = :email")
    List<WeddingEvent> findByHostEmail(@Param("email") String email);

    Optional<WeddingEvent> findBySubdomain(String subdomain);
}

