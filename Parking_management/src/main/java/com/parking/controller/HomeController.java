package com.parking.controller;

import com.parking.model.ParkingLevel;
import com.parking.service.EventService;
import com.parking.service.ParkingService;
import com.parking.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private ParkingService parkingService;

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private EventService eventService;

    @GetMapping("/")
    public String index(Model model) {
        List<ParkingLevel> levels = parkingService.getAllLevels();
        model.addAttribute("levels", levels);
        
        List<Integer> predictions = predictionService.getPredictions();
        // For the home page, we only show the 15-min prediction
        int totalPredictedOccupancy = predictions.isEmpty() ? 0 : predictions.get(0);
        int totalCapacity = levels.stream().mapToInt(ParkingLevel::getTotalSlots).sum();
        int totalAvailablePredicted = Math.max(0, totalCapacity - totalPredictedOccupancy);
        
        model.addAttribute("predictedAvailable", totalAvailablePredicted);
        model.addAttribute("upcomingEvents", eventService.getUpcomingEvents(3)); // next 3 months

        // Check if all levels are full
        boolean allLevelsFull = levels.stream().allMatch(level -> level.getCurrentOccupancy() >= level.getTotalSlots());
        model.addAttribute("allLevelsFull", allLevelsFull);

        return "index";

    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/bike-info")
    public String bikeInfo() {
        return "bike-info";
    }

}