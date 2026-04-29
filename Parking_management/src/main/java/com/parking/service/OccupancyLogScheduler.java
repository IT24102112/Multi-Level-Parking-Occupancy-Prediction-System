package com.parking.service;

import com.parking.model.OccupancyLog;
import com.parking.model.ParkingLevel;
import com.parking.repository.OccupancyLogRepository;
import com.parking.repository.ParkingLevelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Every 10 minutes, takes a snapshot of current parking_level occupancy
 * and writes one record per level to occupancy_log.
 * This feeds the reports module with fresh data automatically.
 * For demo purposes change fixedDelay to 60_000 (1 min) to see
 * the occupancy_log table filling up faster.
 */
@Service
public class OccupancyLogScheduler {

    private static final Logger log = LoggerFactory.getLogger(OccupancyLogScheduler.class);

    @Autowired
    private ParkingLevelRepository parkingLevelRepository;

    @Autowired
    private OccupancyLogRepository occupancyLogRepository;

    @Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
    @Transactional
    public void logOccupancySnapshot() {
        List<ParkingLevel> levels = parkingLevelRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (ParkingLevel level : levels) {
            OccupancyLog entry = new OccupancyLog();
            entry.setLevelName(level.getLevelName());
            entry.setOccupancy(level.getCurrentOccupancy());
            entry.setRecordedAt(now);
            occupancyLogRepository.save(entry);
        }

        log.info("Occupancy snapshot logged at {} for {} levels.", now, levels.size());
    }
}