package com.parking.repository;

import com.parking.model.ParkingLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingLogRepository extends JpaRepository<ParkingLog, Long> {
}
