package com.parking.service;

import com.parking.model.ParkingLevel;
import com.parking.repository.ParkingLevelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingService {

    @Autowired
    private ParkingLevelRepository parkingLevelRepository;

    public List<ParkingLevel> getAllLevels() {
        return parkingLevelRepository.findAll();
    }

    public ParkingLevel getLevel(String levelName) {
        return parkingLevelRepository.findByLevelName(levelName).orElse(null);
    }

    // This method would be called by the IoT system to update occupancy
    public void updateOccupancy(String levelName, int newOccupancy) {
        ParkingLevel level = getLevel(levelName);
        if (level != null) {
            level.setCurrentOccupancy(newOccupancy);
            parkingLevelRepository.save(level);
        }
    }
}