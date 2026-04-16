package com.parking.controller;

import com.parking.dto.PredictionResponse;
import com.parking.model.ParkingLevel;
import com.parking.service.ParkingService;
import com.parking.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @GetMapping("/predictions")
    public Map<String, Integer> getPredictions() {
        return predictionService.getPredictions();
    }
}