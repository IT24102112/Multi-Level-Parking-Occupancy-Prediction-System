package com.parking.dto;

import java.util.List;

public class PredictionResponse {
    private List<Integer> predictions; // Predicted total occupancy for next 15, 30, 45, 60 mins

    public PredictionResponse() {}

    public PredictionResponse(List<Integer> predictions) {
        this.predictions = predictions;
    }

    public List<Integer> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<Integer> predictions) {
        this.predictions = predictions;
    }
}