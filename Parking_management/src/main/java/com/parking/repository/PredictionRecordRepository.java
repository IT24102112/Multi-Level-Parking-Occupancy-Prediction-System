package com.parking.repository;

import com.parking.model.PredictionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PredictionRecordRepository extends JpaRepository<PredictionRecord, Long> {
    PredictionRecord findTopByModelNameOrderByRecordedAtDesc(String modelName);
}

