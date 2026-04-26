package com.parking.service;

import com.parking.model.Sensor;
import com.parking.repository.ParkingLevelRepository;
import com.parking.repository.SensorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SensorService {

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private ParkingLevelRepository parkingLevelRepository;

    // Get all sensors
    public List<Sensor> getAllSensors() {
        return sensorRepository.findAll();
    }

    // Get sensors by level
    public List<Sensor> getSensorsByLevel(Long levelId) {
        return parkingLevelRepository.findById(levelId)
                .map(sensorRepository::findByParkingLevel)
                .orElse(List.of());
    }

    // Get active sensors only
    public List<Sensor> getActiveSensors() {
        return sensorRepository.findByIsActive(true);
    }

    // Record heartbeat from ESP32
    public Sensor recordHeartbeat(Long sensorId) {
        return sensorRepository.findById(sensorId)
                .map(sensor -> {
                    sensor.setLastHeartbeat(LocalDateTime.now());
                    sensor.setActive(true); // mark active when heartbeat received
                    return sensorRepository.save(sensor);
                })
                .orElse(null);
    }

    // Update sensor active status manually or from ESP32
    public Sensor updateSensorStatus(Long sensorId, boolean isActive) {
        return sensorRepository.findById(sensorId)
                .map(sensor -> {
                    sensor.setActive(isActive);
                    sensor.setLastHeartbeat(LocalDateTime.now());
                    return sensorRepository.save(sensor);
                })
                .orElse(null);
    }

    // Create a new sensor
    public Sensor createSensor(Long levelId, String sensorType, boolean isActive) {
        return parkingLevelRepository.findById(levelId)
                .map(level -> {
                    Sensor sensor = new Sensor(level, sensorType, isActive);
                    return sensorRepository.save(sensor);
                })
                .orElse(null);
    }

    // Delete a sensor
    public void deleteSensor(Long sensorId) {
        sensorRepository.deleteById(sensorId);
    }
}