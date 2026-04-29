package com.parking.repository;

import com.parking.model.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PredictionRepository extends JpaRepository<Prediction, Integer> {

    // Most recent predictions (for the AI dashboard table)
    List<Prediction> findTop20ByOrderByPredictionTimeDesc();

    // Predictions where actual occupancy was recorded (for accuracy chart)
    @Query("SELECT p FROM Prediction p WHERE p.actualOccupancy IS NOT NULL ORDER BY p.validForTime ASC")
    List<Prediction> findPredictionsWithActual();

    // Abnormal predictions count
    long countByIsAbnormalTrue();

    // Today's predictions
    List<Prediction> findByPredictionTimeBetweenOrderByPredictionTimeAsc(
            LocalDateTime start, LocalDateTime end);

    List<Prediction> findByActualOccupancyIsNullAndValidForTimeBetween(
            LocalDateTime start, LocalDateTime end);
}