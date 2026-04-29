package com.parking.repository;

import com.parking.model.ParkingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ParkingLogRepository extends JpaRepository<ParkingLog, Long> {

    @Query("SELECT COUNT(p) FROM ParkingLog p WHERE p.direction = 'IN' AND p.eventTime >= :since")
    long countEntriesToday(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(p) FROM ParkingLog p WHERE p.direction = 'OUT' AND p.eventTime >= :since")
    long countExitsToday(@Param("since") LocalDateTime since);}