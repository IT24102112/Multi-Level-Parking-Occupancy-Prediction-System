package com.parking.controller;

import com.parking.model.ParkingLevel;
import com.parking.service.ParkingService;
import com.parking.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private ParkingService parkingService;

    @Autowired
    private PredictionService predictionService;

    @GetMapping("/occupancy")
    public List<ParkingLevel> getOccupancy() {
        return parkingService.getAllLevels();
    }

    @GetMapping("/predict")
    public List<Integer> getAIPrediction() {
        return predictionService.getPredictions();
    }
}