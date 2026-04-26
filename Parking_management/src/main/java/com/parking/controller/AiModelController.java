package com.parking.controller;

import com.parking.service.AiModelService;
import com.parking.service.ParkingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AiModelController {

    @Autowired
    private AiModelService aiModelService;

    @Autowired
    private ParkingService parkingService;

    @GetMapping("/ai-model")
    @PreAuthorize("hasRole('IT_ADMIN')")
    public String aiModelDashboard(Model model) throws Exception {

        // Live occupancy (for current vs predicted comparison)
        model.addAttribute("levels", parkingService.getAllLevels());

        // Recent predictions table
        model.addAttribute("recentPredictions", aiModelService.getRecentPredictions());

        // Stats
        model.addAttribute("totalPredictions", aiModelService.getTotalPredictions());
        model.addAttribute("abnormalCount",    aiModelService.getAbnormalCount());

        // Chart JSON
        model.addAttribute("chartJson", aiModelService.getPredictedVsActualChartJson());

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

        return "admin/ai-model";
    }
}