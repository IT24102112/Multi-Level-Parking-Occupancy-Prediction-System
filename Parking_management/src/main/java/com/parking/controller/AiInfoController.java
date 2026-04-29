package com.parking.controller;

import com.parking.model.ParkingLevel;
import com.parking.service.AiModelService;
import com.parking.service.ParkingService;
import com.parking.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AiInfoController {

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private ParkingService parkingService;

    @GetMapping("/ai-info")
    public String aiInfo(Model model) {
        List<Integer> forecast = predictionService.getPredictions();
        List<ParkingLevel> levels = parkingService.getAllLevels();
        int totalCapacity = levels.stream().mapToInt(ParkingLevel::getTotalSlots).sum();
        
        model.addAttribute("forecast", forecast);
        model.addAttribute("totalCapacity", totalCapacity);
        
        // Static model metadata
        model.addAttribute("seasonalModelType",       AiModelService.SEASONAL_MODEL_TYPE);
        model.addAttribute("seasonalModelAccuracy",   AiModelService.SEASONAL_MODEL_ACCURACY);
        model.addAttribute("seasonalModelMae",        AiModelService.SEASONAL_MODEL_MAE);
        model.addAttribute("seasonalTrainingPeriod",  AiModelService.SEASONAL_TRAINING_PERIOD);
        model.addAttribute("seasonalLastTrained",     AiModelService.SEASONAL_LAST_TRAINED);
        model.addAttribute("seasonalTrainingRecords", AiModelService.SEASONAL_TRAINING_RECORDS);

        model.addAttribute("realtimeModelType",       AiModelService.REALTIME_MODEL_TYPE);
        model.addAttribute("realtimeModelAccuracy",   AiModelService.REALTIME_MODEL_ACCURACY);
        model.addAttribute("realtimeModelMae",        AiModelService.REALTIME_MODEL_MAE);
        model.addAttribute("realtimeTrainingPeriod",  AiModelService.REALTIME_TRAINING_PERIOD);
        model.addAttribute("realtimeLastTrained",     AiModelService.REALTIME_LAST_TRAINED);
        model.addAttribute("realtimeTrainingRecords", AiModelService.REALTIME_TRAINING_RECORDS);

        return "ai-info";
    }
}
