package com.parking.controller;

import com.parking.model.ParkingLevel;
import com.parking.repository.ParkingLevelRepository;
import com.parking.repository.ParkingLogRepository;
import com.parking.service.ParkingSimulatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/simulator")
@PreAuthorize("hasRole('IT_ADMIN')")
public class SimulatorController {

    @Autowired private ParkingSimulatorService simulatorService;
    @Autowired private ParkingLevelRepository  parkingLevelRepository;
    @Autowired private ParkingLogRepository    parkingLogRepository;

    @GetMapping
    public String simulatorPage(Model model) {
        buildModel(model);
        return "admin/simulator";
    }

    @PostMapping("/start")
    public String start() {
        simulatorService.start();
        return "redirect:/admin/simulator";
    }

    @PostMapping("/stop")
    public String stop() {
        simulatorService.stop();
        return "redirect:/admin/simulator";
    }

    @GetMapping("/status")
    @ResponseBody
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("running", simulatorService.isRunning());
        result.put("message", simulatorService.getStatusMessage());
        result.put("target",  simulatorService.getCurrentTarget());

        List<ParkingLevel> levels = parkingLevelRepository.findAll();
        int total = levels.stream().mapToInt(ParkingLevel::getCurrentOccupancy).sum();
        result.put("totalOccupancy", total);

        Map<String, Integer> levelMap = new LinkedHashMap<>();
        for (ParkingLevel l : levels) {
            levelMap.put(l.getLevelName(), l.getCurrentOccupancy());
        }
        result.put("levels", levelMap);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        result.put("entriesToday", parkingLogRepository.countEntriesToday(todayStart));
        result.put("exitsToday",   parkingLogRepository.countExitsToday(todayStart));

        LocalDateTime lastTick = simulatorService.getLastTick();
        result.put("lastTick", lastTick != null
                ? lastTick.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                : "Not yet");

        return result;
    }

    private void buildModel(Model model) {
        List<ParkingLevel> levels = parkingLevelRepository.findAll();
        int total = levels.stream().mapToInt(ParkingLevel::getCurrentOccupancy).sum();

        model.addAttribute("simRunning",   simulatorService.isRunning());
        model.addAttribute("simMessage",   simulatorService.getStatusMessage());
        model.addAttribute("simTarget",    simulatorService.getCurrentTarget());
        model.addAttribute("levels",       levels);
        model.addAttribute("totalOcc",     total);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        model.addAttribute("entriesToday", parkingLogRepository.countEntriesToday(todayStart));
        model.addAttribute("exitsToday",   parkingLogRepository.countExitsToday(todayStart));

        LocalDateTime lastTick = simulatorService.getLastTick();
        model.addAttribute("lastTick", lastTick != null
                ? lastTick.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                : "Not yet");
    }
}