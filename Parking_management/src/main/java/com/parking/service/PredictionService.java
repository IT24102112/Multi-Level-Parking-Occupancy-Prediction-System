package com.parking.service;

import com.parking.dto.PredictionResponse;
import com.parking.model.OccupancyLog;
import com.parking.repository.OccupancyLogRepository;
import com.parking.repository.ParkingLevelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
            // 1. Collect recent history for each level
            List<String> levels = parkingLevelRepository.findAll()
                    .stream().map(l -> l.getLevelName()).collect(Collectors.toList());

            // Map to store lists of occupancy per timestamp
            // index 0 = oldest, index 9 = newest
            List<Integer> totalHistory = new ArrayList<>();
            for (int i = 0; i < 10; i++) totalHistory.add(0);

            for (String level : levels) {
                List<Integer> history = occupancyLogRepository.findTop10ByLevelNameOrderByRecordedAtDesc(level)
                        .stream().map(OccupancyLog::getOccupancy).collect(Collectors.toList());
                Collections.reverse(history); // chronological order
                
                // Add to total history element-wise
                for (int i = 0; i < history.size(); i++) {
                    int reverseIdx = 9 - (history.size() - 1 - i); // Ensure alignment at the end
                    if (reverseIdx >= 0) {
                        totalHistory.set(reverseIdx, totalHistory.get(reverseIdx) + history.get(i));
                    }
                }
            }

            // 2. POST total history to Python API
            Mono<PredictionResponse> response = webClient.post()
                    .uri(apiUrl)
                    .bodyValue(Map.of("data", totalHistory))
                    .retrieve()
                    .bodyToMono(PredictionResponse.class);

            PredictionResponse pred = response.block();
            return pred != null ? pred.getPredictions() : fallbackPredictionsList();

        } catch (Exception e) {
            System.err.println("AI Prediction Error: " + e.getMessage());
            return fallbackPredictionsList();
        }
    }

    private List<Integer> fallbackPredictionsList() {
        // Mocking 4 steps (15/30/45/60 min)
        return Arrays.asList(450, 480, 520, 580);
    }

    // Deprecated but kept for compatibility during transition if needed
    public Map<String, Integer> getPredictionsMap() {
        Map<String, Integer> map = new HashMap<>();
        List<Integer> list = getPredictions();
        map.put("Total", list.get(0));
        return map;
    }
}