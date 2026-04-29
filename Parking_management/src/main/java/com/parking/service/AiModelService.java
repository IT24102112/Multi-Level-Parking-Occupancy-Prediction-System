package com.parking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.model.Prediction;
import com.parking.repository.PredictionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiModelService {

    // ── Static model metadata ─────────────────────────────────────────────
    public static final String SEASONAL_MODEL_TYPE       = "LSTM (Long Short-Term Memory)";
    public static final String SEASONAL_MODEL_ACCURACY   = "94.7%";
    public static final String SEASONAL_MODEL_MAE        = "0.94";
    public static final String SEASONAL_TRAINING_PERIOD  = "2015 – 2025 (10 years)";
    public static final String SEASONAL_LAST_TRAINED     = "April 2026";
    public static final String SEASONAL_TRAINING_RECORDS = "~3.6 million records";

    public static final String REALTIME_MODEL_TYPE       = "SGD (Stochastic Gradient Descent)";
    public static final String REALTIME_MODEL_ACCURACY   = "99.31%";
    public static final String REALTIME_MODEL_MAE        = "1.61";
    public static final String REALTIME_TRAINING_PERIOD  = "Recent IoT + 3 months";
    public static final String REALTIME_LAST_TRAINED     = "April 2026";
    public static final String REALTIME_TRAINING_RECORDS = "~250,000 recent records";
    // ─────────────────────────────────────────────────────────────────────

    @Autowired
    private PredictionRepository predictionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Returns the 20 most recent predictions from the database.
     * Falls back to mock data if the table is empty (model not yet connected).
     */
    public List<Prediction> getRecentPredictions() {
        List<Prediction> real = predictionRepository.findTop20ByOrderByPredictionTimeDesc();
        if (!real.isEmpty()) return real;
        return generateMockPredictions();
    }

    /**
     * Count of abnormal predictions — returns 0 if table is empty.
     */
    public long getAbnormalCount() {
        return predictionRepository.countByIsAbnormalTrue();
    }

    /**
     * Total predictions stored in the database.
     */
    public long getTotalPredictions() {
        long count = predictionRepository.count();
        return count > 0 ? count : 0;
    }

    /**
     * Builds JSON data for the predicted vs actual chart.
     * Uses real data if available, otherwise uses mock data.
     */
    public String getPredictedVsActualChartJson() throws JsonProcessingException {
        List<Prediction> data = predictionRepository.findPredictionsWithActual();

        if (data.isEmpty()) {
            // Generate mock chart data for demo purposes
            data = generateMockPredictionsWithActual();
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM HH:mm");
        List<String> labels = data.stream()
                .map(p -> p.getValidForTime().format(fmt))
                .collect(Collectors.toList());
        List<Integer> predicted = data.stream()
                .map(Prediction::getPredictedOccupancy)
                .collect(Collectors.toList());
        List<Integer> actual = data.stream()
                .map(p -> p.getActualOccupancy() != null ? p.getActualOccupancy() : null)
                .collect(Collectors.toList());

        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("labels", labels);
        chart.put("predicted", predicted);
        chart.put("actual", actual);

        return objectMapper.writeValueAsString(chart);
    }

    // ── Mock data generators (used when prediction table is empty) ────────

    private List<Prediction> generateMockPredictions() {
        List<Prediction> mocks = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().minusHours(5);
        int[] occupancies = {145, 162, 178, 155, 190, 210, 225, 198, 175, 160,
                142, 130, 118, 105, 122, 148, 170, 195, 215, 230};
        for (int i = 0; i < 20; i++) {
            Prediction p = new Prediction();
            p.setId(i + 1);
            p.setPredictedOccupancy(occupancies[i]);
            p.setPredictionTime(base.plusMinutes(i * 15L));
            p.setValidForTime(base.plusMinutes(i * 15L + 15));
            p.setIsAbnormal(occupancies[i] > 220);
            p.setActualOccupancy(occupancies[i] + (int)(Math.random() * 20 - 10));
            mocks.add(p);
        }
        Collections.reverse(mocks);
        return mocks;
    }

    private List<Prediction> generateMockPredictionsWithActual() {
        List<Prediction> mocks = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().minusHours(5);
        int[] pred    = {120, 135, 150, 168, 185, 200, 215, 195, 175, 158,
                140, 125, 110, 128, 145, 165, 182, 200, 218, 235};
        int[] actual  = {115, 140, 148, 172, 180, 208, 210, 192, 178, 152,
                138, 130, 108, 132, 150, 162, 185, 198, 225, 230};
        for (int i = 0; i < 20; i++) {
            Prediction p = new Prediction();
            p.setPredictedOccupancy(pred[i]);
            p.setValidForTime(base.plusMinutes(i * 15L));
            p.setPredictionTime(base.plusMinutes(i * 15L - 15));
            p.setActualOccupancy(actual[i]);
            p.setIsAbnormal(pred[i] > 220);
            mocks.add(p);
        }
        return mocks;
    }
}