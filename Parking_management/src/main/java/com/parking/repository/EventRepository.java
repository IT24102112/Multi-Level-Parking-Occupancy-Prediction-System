package com.parking.repository;

import com.parking.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByStartDateBetween(LocalDateTime start, LocalDateTime end);
}