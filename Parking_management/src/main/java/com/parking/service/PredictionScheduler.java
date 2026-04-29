package com.parking.service;

import com.parking.model.ParkingLevel;
import com.parking.model.Prediction;
import com.parking.repository.ParkingLevelRepository;
import com.parking.repository.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PredictionScheduler
 * ───────────────────
 * Every 10 minutes:
 *   1. Calls PredictionService → hits Flask /predict endpoint
 *   2. Receives [+15min, +30min, +45min, +60min] occupancy predictions
 *   3. Saves each as a separate row in the `prediction` table
 *
 * The `prediction` table is then read by AiModelService for the admin dashboard.
 * Actual occupancy is back-filled separately by OccupancyBackfillService.
 *
 * To enable scheduling, make sure your main class has @EnableScheduling.
 * (already added in ParkingApplication.java patch — see spring_patches folder)
 */
@Component
public class PredictionScheduler {

    private static final Logger log = LoggerFactory.getLogger(PredictionScheduler.class);

    // If total occupancy exceeds this % of capacity, flag as abnormal
    private static final double ABNORMAL_THRESHOLD_PCT = 0.90;


    // inject from DB: use ParkingLevelRepository.findAll().stream().mapToInt(...).sum()
    @Autowired
    private ParkingLevelRepository parkingLevelRepository;
    @Autowired
    private PredictionService predictionService;

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private OccupancyBackfillService occupancyBackfillService;

    /**
     * Runs every 10 minutes. Offset by 30s to avoid clashing with OccupancyLog snapshot.
     * cron = "30 seconds past every 10th minute"
     */
    @Scheduled(cron = "30 0/10 * * * *")
    public void savePredictions() {
        log.info("⏱  PredictionScheduler triggered — fetching predictions from Flask");

        try {
            List<Integer> preds = predictionService.getPredictions();

            if (preds == null || preds.isEmpty()) {
                log.warn("PredictionScheduler: empty response from Flask, skipping DB write");
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            int[] horizons = {15, 30, 45, 60};   // minutes ahead

            for (int i = 0; i < Math.min(preds.size(), horizons.length); i++) {
                int predictedOccupancy = preds.get(i);
                LocalDateTime validFor = now.plusMinutes(horizons[i]);

                Prediction p = new Prediction();
                p.setPredictionTime(now);
                p.setValidForTime(validFor);
                p.setPredictedOccupancy(predictedOccupancy);
                int totalCapacity = parkingLevelRepository.findAll()
                        .stream().mapToInt(ParkingLevel::getTotalSlots).sum();
                p.setIsAbnormal(predictedOccupancy > totalCapacity * ABNORMAL_THRESHOLD_PCT);
                // actualOccupancy left null — back-filled by OccupancyBackfillService

                predictionRepository.save(p);
                log.info("💾  Saved prediction: +{}min → {} vehicles (abnormal={})",
                        horizons[i], predictedOccupancy, p.getIsAbnormal());
            }

            // Immediately try to backfill any past predictions
            occupancyBackfillService.backfillActualOccupancy();

        } catch (Exception e) {
            log.error("PredictionScheduler failed: {}", e.getMessage(), e);
            // Do NOT rethrow — scheduler must not crash Spring Boot
        }
    }

}