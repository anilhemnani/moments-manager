package com.momentsmanager.repository;

import com.momentsmanager.model.EventTrafficLog;
import com.momentsmanager.model.WeddingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventTrafficLogRepository extends JpaRepository<EventTrafficLog, Long> {
    long countByEvent(WeddingEvent event);

    @Query("select e.id as eventId, count(t) as views from EventTrafficLog t join t.event e group by e.id")
    List<Object[]> summarizeViewsPerEvent();

    @Query("select e.id as eventId, max(t.createdAt) as lastView from EventTrafficLog t join t.event e group by e.id")
    List<Object[]> summarizeLastViewPerEvent();
}
