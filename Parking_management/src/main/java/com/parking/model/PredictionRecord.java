package com.parking.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediction_record")
public class PredictionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "predictions", length = 2000)
    private String predictions;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    public PredictionRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getPredictions() { return predictions; }
    public void setPredictions(String predictions) { this.predictions = predictions; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}

