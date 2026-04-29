package com.parking.repository;

import com.parking.model.ParkingLevel;
import com.parking.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SensorRepository extends JpaRepository<Sensor, Long> {
    List<Sensor> findByParkingLevel(ParkingLevel parkingLevel);
    List<Sensor> findByIsActive(boolean isActive);
    List<Sensor> findByParkingLevelAndSensorType(ParkingLevel parkingLevel, String sensorType);
}

