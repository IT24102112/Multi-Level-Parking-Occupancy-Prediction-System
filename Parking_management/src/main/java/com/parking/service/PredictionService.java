package com.parking.service;

import com.parking.model.OccupancyLog;
import com.parking.model.ParkingLevel;
import com.parking.repository.OccupancyLogRepository;
import com.parking.repository.ParkingLevelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PredictionService {

    @Value("${ai.model.api.url}")
    private String apiUrl;

    private final WebClient webClient;

    @Autowired
    private OccupancyLogRepository occupancyLogRepository;

    @Autowired
    private ParkingLevelRepository parkingLevelRepository;

    public PredictionService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public List<Integer> getPredictions() {
        try {
            // 1. Get total capacity from DB
            List<ParkingLevel> levels = parkingLevelRepository.findAll();
            int totalCapacity = levels.stream()
                    .mapToInt(ParkingLevel::getTotalSlots).sum();
            int currentOccupancy = levels.stream()
                    .mapToInt(ParkingLevel::getCurrentOccupancy).sum();

            if (totalCapacity == 0) return fallbackPredictionsList();

            // 2. Get last 3 occupancy snapshots per level, sum to total
            //    occ_lag_1 = 10 min ago, occ_lag_2 = 20 min ago, occ_lag_3 = 30 min ago
            List<String> levelNames = levels.stream()
                    .map(ParkingLevel::getLevelName)
                    .collect(Collectors.toList());

            // Collect last 3 total occupancy snapshots (chronological, oldest first)
            List<Integer> recentTotals = getRecentTotalOccupancies(levelNames, 3);

            // 3. Build feature values
            double occupancyPct   = (currentOccupancy * 100.0) / totalCapacity;

            // Lag values as percentages (10, 20, 30 min ago)
            double occLag1 = recentTotals.size() >= 1
                    ? (recentTotals.get(recentTotals.size() - 1) * 100.0) / totalCapacity
                    : occupancyPct;
            double occLag2 = recentTotals.size() >= 2
                    ? (recentTotals.get(recentTotals.size() - 2) * 100.0) / totalCapacity
                    : occLag1;
            double occLag3 = recentTotals.size() >= 3
                    ? (recentTotals.get(recentTotals.size() - 3) * 100.0) / totalCapacity
                    : occLag2;

            double rollingMean30m = (occLag1 + occLag2 + occLag3) / 3.0;

            LocalDateTime now   = LocalDateTime.now();
            int hour            = now.getHour();
            int dayOfWeek       = now.getDayOfWeek().getValue() - 1; // 0=Mon, 6=Sun
            int isWeekend       = (dayOfWeek >= 5) ? 1 : 0;

            // 4. Build request body matching Flask's expected format exactly
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("occupancy_pct",    round2(occupancyPct));
            requestBody.put("hour",             hour);
            requestBody.put("day_of_week",      dayOfWeek);
            requestBody.put("is_weekend",       isWeekend);
            requestBody.put("occ_lag_1",        round2(occLag1));
            requestBody.put("occ_lag_2",        round2(occLag2));
            requestBody.put("occ_lag_3",        round2(occLag3));
            requestBody.put("rolling_mean_30m", round2(rollingMean30m));

            // 5. Call Flask
            Mono<Map> response = webClient.post()
                    .uri(apiUrl)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class);

            Map result = response.block();

            if (result == null || !result.containsKey("predicted_occupancy_pct")) {
                return fallbackPredictionsList();
            }

            // 6. Convert predicted_occupancy_pct → vehicle count
            double predPct    = ((Number) result.get("predicted_occupancy_pct")).doubleValue();
            int predVehicles  = (int) Math.round((predPct / 100.0) * totalCapacity);
            predVehicles      = Math.max(0, Math.min(predVehicles, totalCapacity));

            // Flask gives 1 prediction (next 15 min).
            // Return same value for all 4 steps — scheduler saves each as separate DB row.
            return Arrays.asList(predVehicles, predVehicles, predVehicles, predVehicles);

        } catch (Exception e) {
            System.err.println("AI Prediction Error: " + e.getMessage());
            return fallbackPredictionsList();
        }
    }

    /**
     * Gets the last N total occupancy readings summed across all levels,
     * ordered oldest first.
     */
    private List<Integer> getRecentTotalOccupancies(List<String> levelNames, int n) {
        // Get last N snapshots timestamps
        // Each snapshot has one row per level with same recorded_at
        // We sum across levels per timestamp
        Map<LocalDateTime, Integer> totalsMap = new TreeMap<>();

        for (String level : levelNames) {
            List<OccupancyLog> logs = occupancyLogRepository
                    .findTop10ByLevelNameOrderByRecordedAtDesc(level);
            for (OccupancyLog log : logs) {
                totalsMap.merge(log.getRecordedAt(), log.getOccupancy(), Integer::sum);
            }
        }

        // Get last N timestamps, oldest first
        List<Integer> result = new ArrayList<>(totalsMap.values());
        int fromIndex = Math.max(0, result.size() - n);
        return result.subList(fromIndex, result.size());
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    private List<Integer> fallbackPredictionsList() {
        return Arrays.asList(450, 480, 520, 580);
    }

    // Kept for compatibility
    public Map<String, Integer> getPredictionsMap() {
        Map<String, Integer> map = new HashMap<>();
        List<Integer> list = getPredictions();
        map.put("Total", list.get(0));
        return map;
    }
}