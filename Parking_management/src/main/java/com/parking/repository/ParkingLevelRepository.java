package com.parking.repository;

import com.parking.model.ParkingLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ParkingLevelRepository extends JpaRepository<ParkingLevel, Long> {
    Optional<ParkingLevel> findByLevelName(String levelName);
}