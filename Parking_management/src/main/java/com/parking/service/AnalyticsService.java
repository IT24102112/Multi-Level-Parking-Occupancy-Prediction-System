package com.parking.service;

import com.parking.model.OccupancyLog;
import com.parking.repository.OccupancyLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    // ── Configurable capacity per level ──────────────────────────────────
    private static final Map<String, Integer> LEVEL_CAPACITY;
    private static final int DEFAULT_CAPACITY = 300;

    static {
        LEVEL_CAPACITY = new LinkedHashMap<>();
        LEVEL_CAPACITY.put("Level 1", 300);
        LEVEL_CAPACITY.put("Level 2", 300);
        LEVEL_CAPACITY.put("Level 3", 300);
        LEVEL_CAPACITY.put("Level 4", 300);
    }

    // Total car park capacity (sum of all levels)
    private static final int TOTAL_CAPACITY =
            LEVEL_CAPACITY.values().stream().mapToInt(Integer::intValue).sum();
    // ─────────────────────────────────────────────────────────────────────

    @Autowired
    private OccupancyLogRepository occupancyLogRepository;

    // ── Existing methods ──────────────────────────────────────────────────

    public List<OccupancyLog> getLogsByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return occupancyLogRepository.findByRecordedAtBetweenOrderByRecordedAtAsc(start, end);
    }

    public List<OccupancyLog> getAllLogs() {
        return occupancyLogRepository.findAllByOrderByRecordedAtDesc();
    }

    public List<OccupancyLog> getLogsByLevelAndDateRange(String levelName,
                                                         LocalDateTime start,
                                                         LocalDateTime end) {
        return occupancyLogRepository
                .findByLevelNameAndRecordedAtBetweenOrderByRecordedAtAsc(levelName, start, end);
    }

    public List<String> getAllLevelNames() {
        return occupancyLogRepository.findDistinctLevelNames();
    }

    public List<OccupancyLog> getLogsByDateRange(LocalDate startDate,
                                                 LocalDate endDate,
                                                 String levelName) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        if (levelName != null && !levelName.isBlank()) {
            return occupancyLogRepository
                    .findByLevelNameAndRecordedAtBetweenOrderByRecordedAtAsc(levelName, start, end);
        }
        return occupancyLogRepository.findByRecordedAtBetweenOrderByRecordedAtAsc(start, end);
    }

    // ── Summary statistics ────────────────────────────────────────────────

    /** Time-weighted average occupancy across all provided logs. */
    public int getAverageOccupancy(List<OccupancyLog> logs) {
        if (logs.isEmpty()) return 0;
        if (logs.size() == 1) return logs.get(0).getOccupancy();

        List<OccupancyLog> sorted = logs.stream()
                .sorted(Comparator.comparing(OccupancyLog::getRecordedAt))
                .collect(Collectors.toList());

        double weightedSum = 0;
        long totalSeconds = 0;
        for (int i = 0; i < sorted.size() - 1; i++) {
            long duration = java.time.Duration.between(
                    sorted.get(i).getRecordedAt(),
                    sorted.get(i + 1).getRecordedAt()).getSeconds();
            weightedSum += sorted.get(i).getOccupancy() * duration;
            totalSeconds += duration;
        }
        weightedSum += sorted.get(sorted.size() - 1).getOccupancy();
        totalSeconds += 1;
        return (int) Math.round(weightedSum / totalSeconds);
    }

    /** Peak occupancy value. */
    public int getPeakOccupancy(List<OccupancyLog> logs) {
        return logs.stream().mapToInt(OccupancyLog::getOccupancy).max().orElse(0);
    }

    /** Level name with highest single reading. */
    public String getBusiestLevel(List<OccupancyLog> logs) {
        return logs.stream()
                .max(Comparator.comparingInt(OccupancyLog::getOccupancy))
                .map(OccupancyLog::getLevelName)
                .orElse("N/A");
    }

    /** Hour of day (0-23) with highest average occupancy. Returns -1 if empty. */
    public int getPeakHour(List<OccupancyLog> logs) {
        if (logs.isEmpty()) return -1;
        Map<Integer, Double> avgByHour = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getRecordedAt().getHour(),
                        Collectors.averagingInt(OccupancyLog::getOccupancy)));
        return avgByHour.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);
    }

    /**
     * NEW: Average utilisation as a percentage of total capacity.
     * Uses the sum of all level capacities as the denominator so that
     * when filtering to a single level it still reflects that level's usage.
     */
    public int getAvgUtilisationPercent(List<OccupancyLog> logs, String levelFilter) {
        int avg = getAverageOccupancy(logs);
        int capacity = (levelFilter != null && !levelFilter.isBlank())
                ? getCapacityForLevel(levelFilter)
                : TOTAL_CAPACITY;
        return (int) Math.min(100, Math.round((avg * 100.0) / capacity));
    }

    /**
     * NEW: Returns the exact datetime when the peak occupancy occurred,
     * formatted as a human-readable string e.g. "Friday 26 Dec 2025 at 14:00".
     */
    public String getPeakDateTimeLabel(List<OccupancyLog> logs) {
        if (logs.isEmpty()) return "N/A";
        return logs.stream()
                .max(Comparator.comparingInt(OccupancyLog::getOccupancy))
                .map(l -> l.getRecordedAt().format(
                        DateTimeFormatter.ofPattern("EEEE dd MMM yyyy 'at' HH:mm")))
                .orElse("N/A");
    }

    /**
     * NEW: Plain-English insight sentence for the admin.
     * e.g. "Average utilisation was 74% — peak demand hit on Friday 26 Dec at 14:00."
     */
    public String getInsightSentence(List<OccupancyLog> logs,
                                     int avgUtilPct,
                                     String peakDateTimeLabel,
                                     String busiestLevel) {
        if (logs.isEmpty()) return null;
        String util = avgUtilPct >= 80 ? "⚠ High utilisation (" + avgUtilPct + "%)" :
                avgUtilPct >= 50 ? "Moderate utilisation (" + avgUtilPct + "%)" :
                        "Low utilisation (" + avgUtilPct + "%)";
        return util + " across the selected period — peak demand recorded on "
                + peakDateTimeLabel + ". " + busiestLevel + " was the busiest level.";
    }

    /** Capacity for a given level (falls back to DEFAULT_CAPACITY). */
    public int getCapacityForLevel(String levelName) {
        return LEVEL_CAPACITY.getOrDefault(levelName, DEFAULT_CAPACITY);
    }

    /** Occupancy as % of level capacity, capped at 100. */
    public int getOccupancyPercent(String levelName, int occupancy) {
        int capacity = getCapacityForLevel(levelName);
        return (int) Math.min(100, Math.round((occupancy * 100.0) / capacity));
    }

    // ── Chart data helpers ────────────────────────────────────────────────

    public Map<String, Object> getTrendChartData(List<OccupancyLog> logs,
                                                 LocalDate startDate,
                                                 LocalDate endDate) {
        long daySpan = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        boolean bucketByHour = daySpan > 2;

        DateTimeFormatter fmt = bucketByHour
                ? DateTimeFormatter.ofPattern("dd MMM HH:mm")
                : DateTimeFormatter.ofPattern("HH:mm");

        Map<String, List<OccupancyLog>> byLevel = logs.stream()
                .collect(Collectors.groupingBy(
                        OccupancyLog::getLevelName, TreeMap::new, Collectors.toList()));

        LinkedHashSet<String> labelSet = new LinkedHashSet<>();
        for (List<OccupancyLog> lvLogs : byLevel.values()) {
            lvLogs.stream()
                    .map(l -> bucketByHour
                            ? l.getRecordedAt().withMinute(0).withSecond(0).format(fmt)
                            : l.getRecordedAt().format(fmt))
                    .forEach(labelSet::add);
        }
        List<String> labels = new ArrayList<>(labelSet);

        Map<String, List<Integer>> datasets = new LinkedHashMap<>();
        for (Map.Entry<String, List<OccupancyLog>> entry : byLevel.entrySet()) {
            String level = entry.getKey();
            Map<String, Double> bucketAvg = entry.getValue().stream()
                    .collect(Collectors.groupingBy(
                            l -> bucketByHour
                                    ? l.getRecordedAt().withMinute(0).withSecond(0).format(fmt)
                                    : l.getRecordedAt().format(fmt),
                            Collectors.averagingInt(OccupancyLog::getOccupancy)));
            List<Integer> values = labels.stream()
                    .map(lbl -> bucketAvg.containsKey(lbl)
                            ? (int) Math.round(bucketAvg.get(lbl)) : null)
                    .collect(Collectors.toList());
            datasets.put(level, values);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("datasets", datasets);
        return result;
    }

    public Map<String, Object> getLevelBarChartData(List<OccupancyLog> logs) {
        Map<String, Double> avgByLevel = logs.stream()
                .collect(Collectors.groupingBy(
                        OccupancyLog::getLevelName, TreeMap::new,
                        Collectors.averagingInt(OccupancyLog::getOccupancy)));

        List<String> labels = new ArrayList<>(avgByLevel.keySet());
        List<Integer> values = labels.stream()
                .map(l -> (int) Math.round(avgByLevel.get(l)))
                .collect(Collectors.toList());
        List<Integer> capacities = labels.stream()
                .map(this::getCapacityForLevel)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("values", values);
        result.put("capacities", capacities);
        return result;
    }
}