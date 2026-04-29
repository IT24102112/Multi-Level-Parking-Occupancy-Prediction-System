package com.parking.service;

import com.parking.model.OccupancyLog;
import com.parking.model.ParkingLevel;
import com.parking.repository.OccupancyLogRepository;
import com.parking.repository.ParkingLevelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class ParkingService {

    @Autowired
    private ParkingLevelRepository parkingLevelRepository;

    @Autowired
    private OccupancyLogRepository occupancyLogRepository;

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

    @Transactional
    public ParkingLevel processSensorEvent(String levelName, String entryExit) {

        ParkingLevel level = parkingLevelRepository
                .findByLevelName(levelName)
                .orElseThrow(() -> new RuntimeException("Level not found"));

        int current = level.getCurrentOccupancy();
        int total = level.getTotalSlots();

        // Normalize entryExit to handle various payloads (e.g., "in","out","ENTRY","EXIT","entrance", etc.)
        String normalized = entryExit == null ? "" : entryExit.trim().toLowerCase();

        List<String> entrySynonyms = Arrays.asList("in", "entry", "entrance", "enter", "1");
        List<String> exitSynonyms = Arrays.asList("out", "exit", "leave", "egress", "0");

        boolean isEntry = entrySynonyms.contains(normalized);
        boolean isExit = exitSynonyms.contains(normalized);

        // Fallback heuristics: if it starts with 'e', decide based on presence of 'xit'
        if (!isEntry && !isExit && normalized.startsWith("e")) {
            if (normalized.contains("xit")) isExit = true;
            else isEntry = true;
        }

        if (isEntry) {
            if (current < total) {
                level.setCurrentOccupancy(current + 1);
            }
        } else if (isExit) {
            if (current > 0) {
                level.setCurrentOccupancy(current - 1);
            }
        } else {
            throw new RuntimeException("Invalid entryExit value: " + entryExit);
        }

        ParkingLevel savedLevel = parkingLevelRepository.save(level);

        // Save occupancy log entry with timestamp
        OccupancyLog log = new OccupancyLog(
                savedLevel.getLevelName(),
                savedLevel.getCurrentOccupancy(),
                LocalDateTime.now()
        );
        occupancyLogRepository.save(log);

        return savedLevel;
    }
}