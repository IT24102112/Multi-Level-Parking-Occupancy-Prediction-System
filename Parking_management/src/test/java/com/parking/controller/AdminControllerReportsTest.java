package com.parking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.model.OccupancyLog;
import com.parking.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerReportsTest {

    private MockMvc mockMvc;

    @Mock
    private ParkingService parkingService;

    @Mock
    private PredictionService predictionService;

    @Mock
    private EventService eventService;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private SystemStatusService systemStatusService;

    @InjectMocks
    private AdminController adminController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    @Test
    void reports_WithValidParameters_ReturnsReportsPage() throws Exception {
        // Prepare mock data
        LocalDate startDate = LocalDate.of(2026, 3, 18);
        LocalDate endDate = LocalDate.of(2026, 3, 24);
        List<OccupancyLog> logs = Arrays.asList(
                new OccupancyLog("Level 1", 120, LocalDateTime.now()),
                new OccupancyLog("Level 2", 200, LocalDateTime.now())
        );
        List<String> levelNames = Arrays.asList("Level 1", "Level 2", "Level 3", "Level 4");

        when(analyticsService.getLogsByDateRange(any(), any(), any())).thenReturn(logs);
        when(analyticsService.getAverageOccupancy(any())).thenReturn(160);
        when(analyticsService.getPeakOccupancy(any())).thenReturn(200);
        when(analyticsService.getPeakHour(any())).thenReturn(14);
        when(analyticsService.getBusiestLevel(any())).thenReturn("Level 2");
        when(analyticsService.getAllLevelNames()).thenReturn(levelNames);
        when(analyticsService.getTrendChartData(any(), any(), any())).thenReturn(Map.of("labels", List.of(), "datasets", Map.of()));
        when(analyticsService.getLevelBarChartData(any())).thenReturn(Map.of("labels", List.of(), "values", List.of(), "capacities", List.of()));

        mockMvc.perform(get("/admin/reports")
                        .param("startDate", "2026-03-18")
                        .param("endDate", "2026-03-24")
                        .param("levelFilter", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports"))
                .andExpect(model().attributeExists("logs", "startDate", "endDate", "avgOccupancy", "peakOccupancy", "busiestLevel", "totalLogs"));
    }

    @Test
    void reports_WhenEndDateBeforeStartDate_ShowsErrorMessage() throws Exception {
        mockMvc.perform(get("/admin/reports")
                        .param("startDate", "2026-03-24")
                        .param("endDate", "2026-03-18"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void reports_WhenStartDateInFuture_ShowsErrorMessage() throws Exception {
        String futureDate = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(get("/admin/reports")
                        .param("startDate", futureDate)
                        .param("endDate", futureDate))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void kmcReports_WithValidParameters_ReturnsReportsPage() throws Exception {
        // Same as reports() but for KMC endpoint
        LocalDate startDate = LocalDate.of(2026, 3, 18);
        LocalDate endDate = LocalDate.of(2026, 3, 24);
        List<OccupancyLog> logs = Arrays.asList(
                new OccupancyLog("Level 1", 120, LocalDateTime.now()),
                new OccupancyLog("Level 2", 200, LocalDateTime.now())
        );
        List<String> levelNames = Arrays.asList("Level 1", "Level 2", "Level 3", "Level 4");

        when(analyticsService.getLogsByDateRange(any(), any(), any())).thenReturn(logs);
        when(analyticsService.getAverageOccupancy(any())).thenReturn(160);
        when(analyticsService.getPeakOccupancy(any())).thenReturn(200);
        when(analyticsService.getPeakHour(any())).thenReturn(14);
        when(analyticsService.getBusiestLevel(any())).thenReturn("Level 2");
        when(analyticsService.getAllLevelNames()).thenReturn(levelNames);
        when(analyticsService.getTrendChartData(any(), any(), any())).thenReturn(Map.of("labels", List.of(), "datasets", Map.of()));
        when(analyticsService.getLevelBarChartData(any())).thenReturn(Map.of("labels", List.of(), "values", List.of(), "capacities", List.of()));

        mockMvc.perform(get("/admin/kmc/reports")
                        .param("startDate", "2026-03-18")
                        .param("endDate", "2026-03-24")
                        .param("levelFilter", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports"))
                .andExpect(model().attributeExists("logs", "startDate", "endDate", "avgOccupancy", "peakOccupancy", "busiestLevel", "totalLogs"));
    }
}