package com.parking.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.model.AppUser;
import com.parking.model.Event;
import com.parking.model.OccupancyLog;
import com.parking.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private ParkingService parkingService;
    @Autowired private PredictionService predictionService;
    @Autowired private EventService eventService;
    @Autowired private AnalyticsService analyticsService;
    @Autowired private SystemStatusService systemStatusService;
    @Autowired private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Dashboard ─────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('IT_ADMIN')")
    public String dashboard(Model model) {
        List<com.parking.model.ParkingLevel> levels = parkingService.getAllLevels();
        model.addAttribute("levels", levels);
        
        List<Integer> predictions = predictionService.getPredictions();
        int totalPredictedOccupancy = predictions.isEmpty() ? 0 : predictions.get(0);
        int totalCapacity = levels.stream().mapToInt(com.parking.model.ParkingLevel::getTotalSlots).sum();
        int totalAvailablePredicted = Math.max(0, totalCapacity - totalPredictedOccupancy);
        
        model.addAttribute("predictedAvailable", totalAvailablePredicted);
        return "admin/dashboard";
    }

    // ── Status ────────────────────────────────────────────────────────────

    @GetMapping("/status")
    @PreAuthorize("hasRole('IT_ADMIN')")
    public String systemStatus(Model model) {
        model.addAttribute("sensorStatuses", systemStatusService.getSensorStatuses());
        model.addAttribute("dbStatus",       systemStatusService.getDatabaseStatus());
        model.addAttribute("modelStatus",    systemStatusService.getModelStatus());
        model.addAttribute("lastChecked",    systemStatusService.getLastChecked());
        return "admin/status";
    }

    // ── Reports (IT Admin) ────────────────────────────────────────────────

    @GetMapping("/reports")
    @PreAuthorize("hasRole('IT_ADMIN')")
    public String reports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String levelFilter,
            Model model,
            HttpServletResponse response) throws JsonProcessingException {

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        buildReportsModel(startDate, endDate, levelFilter, model, "/admin/reports");
        return "admin/reports";
    }

    // ── Reports (KMC Admin) ───────────────────────────────────────────────

    @GetMapping("/kmc/reports")
    @PreAuthorize("hasRole('KMC_ADMIN')")
    public String kmcReports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String levelFilter,
            Model model,
            HttpServletResponse response) throws JsonProcessingException {

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        buildReportsModel(startDate, endDate, levelFilter, model, "/admin/kmc/reports");
        return "admin/reports";
    }

    // ── Shared reports model builder ──────────────────────────────────────

    private void buildReportsModel(LocalDate startDate, LocalDate endDate,
                                   String levelFilter, Model model,
                                   String formAction) throws JsonProcessingException {

        if (startDate == null) startDate = LocalDate.now();
        if (endDate   == null) endDate   = startDate;

        if (endDate.isBefore(startDate)) {
            setErrorModel(model, formAction, startDate, endDate, levelFilter,
                    "The 'To' date cannot be before the 'From' date. Please select a valid date range.");
            return;
        }
        if (startDate.isAfter(LocalDate.now())) {
            setErrorModel(model, formAction, startDate, endDate, levelFilter,
                    "The 'From' date cannot be in the future. Please select a valid date range.");
            return;
        }

        List<OccupancyLog> filteredLogs =
                analyticsService.getLogsByDateRange(startDate, endDate, levelFilter);

        int    avgOccupancy      = analyticsService.getAverageOccupancy(filteredLogs);
        int    peakOccupancy     = analyticsService.getPeakOccupancy(filteredLogs);
        int    peakHour          = analyticsService.getPeakHour(filteredLogs);
        String busiestLevel      = analyticsService.getBusiestLevel(filteredLogs);
        int    avgUtilPct        = analyticsService.getAvgUtilisationPercent(filteredLogs, levelFilter);
        String peakDateTimeLabel = analyticsService.getPeakDateTimeLabel(filteredLogs);
        String insightSentence   = analyticsService.getInsightSentence(
                filteredLogs, avgUtilPct, peakDateTimeLabel, busiestLevel);

        String peakHourLabel = peakHour >= 0
                ? String.format("%02d:00 – %02d:00", peakHour, peakHour + 1) : "N/A";

        Map<String, Object> trendData = analyticsService.getTrendChartData(filteredLogs, startDate, endDate);
        Map<String, Object> barData   = analyticsService.getLevelBarChartData(filteredLogs);
        String trendChartJson = objectMapper.writeValueAsString(trendData);
        String barChartJson   = objectMapper.writeValueAsString(barData);

        model.addAttribute("logs",               filteredLogs);
        model.addAttribute("startDate",          startDate);
        model.addAttribute("endDate",            endDate);
        model.addAttribute("levelFilter",        levelFilter != null ? levelFilter : "");
        model.addAttribute("levelNames",         analyticsService.getAllLevelNames());
        model.addAttribute("formAction",         formAction);
        model.addAttribute("avgOccupancy",       avgOccupancy);
        model.addAttribute("peakOccupancy",      peakOccupancy);
        model.addAttribute("peakHourLabel",      peakHourLabel);
        model.addAttribute("busiestLevel",       busiestLevel);
        model.addAttribute("totalLogs",          filteredLogs.size());
        model.addAttribute("avgUtilPct",         avgUtilPct);
        model.addAttribute("peakDateTimeLabel",  peakDateTimeLabel);
        model.addAttribute("insightSentence",    insightSentence);
        model.addAttribute("trendChartJson",     trendChartJson);
        model.addAttribute("barChartJson",       barChartJson);
    }

    private void setErrorModel(Model model, String formAction,
                               LocalDate startDate, LocalDate endDate,
                               String levelFilter, String message) {
        model.addAttribute("errorMessage",  message);
        model.addAttribute("logs",          Collections.emptyList());
        model.addAttribute("totalLogs",     0);
        model.addAttribute("levelNames",    analyticsService.getAllLevelNames());
        model.addAttribute("formAction",    formAction);
        model.addAttribute("startDate",     startDate);
        model.addAttribute("endDate",       endDate);
        model.addAttribute("levelFilter",   levelFilter != null ? levelFilter : "");
        model.addAttribute("trendChartJson", "{}");
        model.addAttribute("barChartJson",   "{}");
    }

    // ── Events ────────────────────────────────────────────────────────────

    @GetMapping("/events")
    @PreAuthorize("hasRole('IT_ADMIN')")
    public String listEvents(Model model) {
        model.addAttribute("events", eventService.getAllEvents());
        return "admin/events";
    }

    @GetMapping("/events/new")
    @PreAuthorize("hasRole('IT_ADMIN')")
    public String newEventForm(Model model) {
        model.addAttribute("event", new Event());
        return "admin/event-form";
    }

    @GetMapping("/events/edit/{id}")
    @PreAuthorize("hasRole('IT_ADMIN')")
    public String editEventForm(@PathVariable Long id, Model model) {
        Event event = eventService.getEvent(id);
        if (event == null) return "redirect:/admin/events";
        model.addAttribute("event", event);
        return "admin/event-form";
    }

    @PostMapping("/events/save")
    @PreAuthorize("hasRole('IT_ADMIN')")
    public String saveEvent(@Valid @ModelAttribute("event") Event event, BindingResult result) {
        if (result.hasErrors()) return "admin/event-form";
        eventService.saveEvent(event);
        return "redirect:/admin/events";
    }

    @GetMapping("/events/delete/{id}")
    @PreAuthorize("hasRole('IT_ADMIN')")
    public String deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return "redirect:/admin/events";
    }

    // ── Billing ───────────────────────────────────────────────────────────

    @GetMapping("/billing")
    @PreAuthorize("hasRole('IT_ADMIN')")
    public String billing(Model model) {
        List<AppUser> activeUsers = userService.getUsersWithActivePlans();
        model.addAttribute("activeUsers", activeUsers);
        return "admin/billing";
    }

    @GetMapping("/bill/{username}")
    @PreAuthorize("hasRole('IT_ADMIN')")
    public String viewBill(@PathVariable String username, Model model) {
        AppUser user = userService.findByUsername(username);
        if (user == null || user.getPlanEndDate() == null
                || user.getPlanEndDate().isBefore(LocalDateTime.now())) {
            return "redirect:/admin/billing?error=No active plan";
        }
        double amount = userService.calculateBillAmount(user);
        model.addAttribute("user",   user);
        model.addAttribute("amount", amount);
        return "admin/bill";
    }

    
}