package com.parking.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediction")
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "predicted_occupancy", nullable = false)
    private int predictedOccupancy;

    @Column(name = "prediction_time", nullable = false)
    private LocalDateTime predictionTime;

    @Column(name = "valid_for_time", nullable = false)
    private LocalDateTime validForTime;

    @Column(name = "is_abnormal")
    private Boolean isAbnormal = false;

    @Column(name = "actual_occupancy")
    private Integer actualOccupancy;

    public Prediction() {}

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public int getPredictedOccupancy() { return predictedOccupancy; }
    public void setPredictedOccupancy(int predictedOccupancy) { this.predictedOccupancy = predictedOccupancy; }

    public LocalDateTime getPredictionTime() { return predictionTime; }
    public void setPredictionTime(LocalDateTime predictionTime) { this.predictionTime = predictionTime; }

    public LocalDateTime getValidForTime() { return validForTime; }
    public void setValidForTime(LocalDateTime validForTime) { this.validForTime = validForTime; }

    public Boolean getIsAbnormal() { return isAbnormal; }
    public void setIsAbnormal(Boolean isAbnormal) { this.isAbnormal = isAbnormal; }

    public Integer getActualOccupancy() { return actualOccupancy; }
    public void setActualOccupancy(Integer actualOccupancy) { this.actualOccupancy = actualOccupancy; }
}