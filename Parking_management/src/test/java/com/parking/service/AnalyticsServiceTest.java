package com.parking.service;

import com.parking.model.OccupancyLog;
import com.parking.repository.OccupancyLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

    @Mock
    private OccupancyLogRepository occupancyLogRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private List<OccupancyLog> sampleLogs;

    @BeforeEach
    void setUp() {
        sampleLogs = List.of(
                new OccupancyLog("Level 1", 30,  LocalDateTime.of(2025, 1, 1, 9,  0)),
                new OccupancyLog("Level 1", 70,  LocalDateTime.of(2025, 1, 1, 12, 0)),
                new OccupancyLog("Level 1", 90,  LocalDateTime.of(2025, 1, 1, 18, 0)),
                new OccupancyLog("Level 2", 20,  LocalDateTime.of(2025, 1, 1, 9,  0)),
                new OccupancyLog("Level 2", 50,  LocalDateTime.of(2025, 1, 1, 12, 0)),
                new OccupancyLog("Level 2", 60,  LocalDateTime.of(2025, 1, 1, 18, 0))
        );
    }

    // getAverageOccupancy

    @Test
    void averageOccupancy_returnsCorrectAverage() {
        assertEquals(53, analyticsService.getAverageOccupancy(sampleLogs));
    }

    @Test
    void averageOccupancy_emptyList_returnsZero() {
        assertEquals(0, analyticsService.getAverageOccupancy(Collections.emptyList()));
    }

    @Test
    void averageOccupancy_singleLog_returnsThatValue() {
        List<OccupancyLog> single = List.of(
                new OccupancyLog("Level 1", 60, LocalDateTime.now()));
        assertEquals(60, analyticsService.getAverageOccupancy(single));
    }

    // getPeakOccupancy

    @Test
    void peakOccupancy_returnsHighestValue() {
        assertEquals(90, analyticsService.getPeakOccupancy(sampleLogs));
    }

    @Test
    void peakOccupancy_emptyList_returnsZero() {
        assertEquals(0, analyticsService.getPeakOccupancy(Collections.emptyList()));
    }

    // ── getBusiestLevel ───────────────────────────────────────────────────

    @Test
    void busiestLevel_returnsLevelWithHighestReading() {
        assertEquals("Level 1", analyticsService.getBusiestLevel(sampleLogs));
    }

    @Test
    void busiestLevel_emptyList_returnsNA() {
        assertEquals("N/A", analyticsService.getBusiestLevel(Collections.emptyList()));
    }

    // ── getPeakHour ───────────────────────────────────────────────────────

    @Test
    void peakHour_returnsHourWithHighestAverageOccupancy() {
        assertEquals(18, analyticsService.getPeakHour(sampleLogs));
    }

    @Test
    void peakHour_emptyList_returnsMinusOne() {
        assertEquals(-1, analyticsService.getPeakHour(Collections.emptyList()));
    }

    // ── getOccupancyPercent ───────────────────────────────────────────────

    @Test
    void occupancyPercent_calculatesCorrectly() {
        assertEquals(75, analyticsService.getOccupancyPercent("Level 1", 75));
    }

    @Test
    void occupancyPercent_zeroOccupancy_returnsZero() {
        assertEquals(0, analyticsService.getOccupancyPercent("Level 1", 0));
    }

    @Test
    void occupancyPercent_exactCapacity_returns100() {
        assertEquals(100, analyticsService.getOccupancyPercent("Level 1", 250));
    }

    @Test
    void occupancyPercent_cappedAt100WhenOverCapacity() {
        assertEquals(100, analyticsService.getOccupancyPercent("Level 1", 999));
    }

    @Test
    void occupancyPercent_unknownLevel_usesDefaultCapacity() {
        assertEquals(50, analyticsService.getOccupancyPercent("Level 99", 50));
    }

    // ── getLogsByDateRange ────────────────────────────────────────────────

    @Test
    void getLogsByDateRange_allLevels_callsCorrectRepository() {
        when(occupancyLogRepository.findByRecordedAtBetweenOrderByRecordedAtAsc(any(), any()))
                .thenReturn(sampleLogs);

        List<OccupancyLog> result = analyticsService.getLogsByDateRange(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1), null);

        assertEquals(6, result.size());
    }

    @Test
    void getLogsByDateRange_blankLevelFilter_treatedAsAllLevels() {
        when(occupancyLogRepository.findByRecordedAtBetweenOrderByRecordedAtAsc(any(), any()))
                .thenReturn(sampleLogs);

        List<OccupancyLog> result = analyticsService.getLogsByDateRange(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1), "");

        assertEquals(6, result.size());
    }

    @Test
    void getLogsByDateRange_sameDateForStartAndEnd_returnsLogsForThatDay() {
        when(occupancyLogRepository.findByRecordedAtBetweenOrderByRecordedAtAsc(any(), any()))
                .thenReturn(sampleLogs);

        List<OccupancyLog> result = analyticsService.getLogsByDateRange(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1), null);

        assertNotNull(result);
    }

    @Test
    void getLogsByDateRange_withLevelFilter_returnsOnlyThatLevel() {
        List<OccupancyLog> level1Logs = sampleLogs.stream()
                .filter(l -> l.getLevelName().equals("Level 1"))
                .collect(Collectors.toList());

        when(occupancyLogRepository.findByLevelNameAndRecordedAtBetweenOrderByRecordedAtAsc(
                any(), any(), any())).thenReturn(level1Logs);

        List<OccupancyLog> result = analyticsService.getLogsByDateRange(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1), "Level 1");

        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(l -> l.getLevelName().equals("Level 1")));
    }

    // getTrendChartData

    @Test
    void trendChartData_containsLabelsAndDatasets() {
        Map<String, Object> result = analyticsService.getTrendChartData(
                sampleLogs, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1));

        assertTrue(result.containsKey("labels"));
        assertTrue(result.containsKey("datasets"));
    }

    @Test
    void trendChartData_emptyLogs_returnsEmptyLabelsAndDatasets() {
        Map<String, Object> result = analyticsService.getTrendChartData(
                Collections.emptyList(), LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1));

        assertTrue(((List<?>) result.get("labels")).isEmpty());
        assertTrue(((Map<?, ?>) result.get("datasets")).isEmpty());
    }

    @Test
    void trendChartData_singleDayRange_usesTimeFormat() {
        Map<String, Object> result = analyticsService.getTrendChartData(
                sampleLogs, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1));

        List<?> labels = (List<?>) result.get("labels");
        assertFalse(labels.isEmpty());
        assertTrue(labels.get(0).toString().matches("\\d{2}:\\d{2}"));
    }

    @Test
    void trendChartData_multiDayRange_usesDateTimeFormat() {
        Map<String, Object> result = analyticsService.getTrendChartData(
                sampleLogs, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 5));

        List<?> labels = (List<?>) result.get("labels");
        assertFalse(labels.isEmpty());
        assertTrue(labels.get(0).toString().matches("\\d{2} [A-Za-z]{3} \\d{2}:\\d{2}"));
    }

    // getLevelBarChartData

    @Test
    void barChartData_containsCorrectLevelCount() {
        Map<String, Object> result = analyticsService.getLevelBarChartData(sampleLogs);
        assertEquals(2, ((List<?>) result.get("labels")).size());
    }

    @Test
    void barChartData_emptyLogs_returnsEmptyLists() {
        Map<String, Object> result = analyticsService.getLevelBarChartData(Collections.emptyList());
        assertTrue(((List<?>) result.get("labels")).isEmpty());
        assertTrue(((List<?>) result.get("values")).isEmpty());
    }
}
