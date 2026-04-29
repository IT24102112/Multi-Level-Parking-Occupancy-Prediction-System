package com.parking.service;

import com.parking.model.Prediction;
import com.parking.repository.OccupancyLogRepository;
import com.parking.repository.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OccupancyBackfillService
 * ────────────────────────
 * Runs every 10 minutes (offset by 5 min from PredictionScheduler).
 *
 * Purpose:
 *   When a prediction was made for time T, the actual occupancy at T
 *   is only known AFTER time T passes. This service looks back at predictions
 *   whose validForTime has now passed but still have actualOccupancy = NULL,
 *   and fills in the real occupancy from the occupancy_log table.
 *
 * This is what populates the "Predicted vs Actual" accuracy chart in the admin dashboard.
 */
@Component
public class OccupancyBackfillService {

    private static final Logger log = LoggerFactory.getLogger(OccupancyBackfillService.class);

    // How many minutes tolerance when matching prediction time to occupancy log time
    private static final int MATCH_TOLERANCE_MINUTES = 12;

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private OccupancyLogRepository occupancyLogRepository;

    /**
     * Runs at 5 minutes past every 10-minute mark.
     * e.g., :05, :15, :25, :35, :45, :55
     */
    @Scheduled(fixedDelay = 120_000, initialDelay = 30_000)
    public void backfillActualOccupancy() {
        log.info("🔄  OccupancyBackfillService: checking for predictions needing actual occupancy");

        try {
            LocalDateTime now          = LocalDateTime.now();
            LocalDateTime lookbackFrom = now.minusHours(3);

            List<Prediction> unfilled = predictionRepository
                    .findByActualOccupancyIsNullAndValidForTimeBetween(lookbackFrom, now);

            if (unfilled.isEmpty()) {
                log.info("OccupancyBackfillService: nothing to backfill");
                return;
            }

            log.info("OccupancyBackfillService: {} predictions need backfill", unfilled.size());

            for (Prediction pred : unfilled) {
                LocalDateTime targetTime  = pred.getValidForTime();
                LocalDateTime windowStart = targetTime.minusMinutes(MATCH_TOLERANCE_MINUTES);
                LocalDateTime windowEnd   = targetTime.plusMinutes(MATCH_TOLERANCE_MINUTES);

                // Step 1: find the single nearest snapshot timestamp within the window
                LocalDateTime nearestTime = occupancyLogRepository
                        .findNearestSnapshotTime(targetTime, windowStart, windowEnd);

                if (nearestTime == null) {
                    log.warn("⚠️  No snapshot found for id={} validFor={}", pred.getId(), targetTime);
                    continue;
                }

                // Step 2: sum all levels at that exact snapshot only (±1 second)
                Integer totalActual = occupancyLogRepository
                        .sumOccupancyByRecordedAtBetween(
                                nearestTime.minusSeconds(1),
                                nearestTime.plusSeconds(1));

                if (totalActual != null && totalActual > 0) {
                    pred.setActualOccupancy(totalActual);
                    predictionRepository.save(pred);
                    log.info("✅  Backfilled id={} validFor={} nearestSnapshot={} actual={}",
                            pred.getId(), targetTime, nearestTime, totalActual);
                }
            }

        } catch (Exception e) {
            log.error("OccupancyBackfillService failed: {}", e.getMessage(), e);
        }
    }
    }

