package com.parking.repository;

import com.parking.model.OccupancyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OccupancyLogRepository extends JpaRepository<OccupancyLog, Long> {

    List<OccupancyLog> findByLevelNameAndRecordedAtBetweenOrderByRecordedAtAsc(
            String levelName, LocalDateTime start, LocalDateTime end);

    List<OccupancyLog> findAllByOrderByRecordedAtDesc();

    @Query("SELECT DISTINCT o.levelName FROM OccupancyLog o ORDER BY o.levelName")
    List<String> findDistinctLevelNames();

    // New method for date-only filter
    List<OccupancyLog> findByRecordedAtBetweenOrderByRecordedAtAsc(LocalDateTime start, LocalDateTime end);

    List<OccupancyLog> findTop10ByLevelNameOrderByRecordedAtDesc(String levelName);

    /**
     * Finds the nearest snapshot timestamp to the target within the window.
     */
    @Query(value = "SELECT TOP 1 recorded_at FROM occupancy_log " +
            "WHERE recorded_at BETWEEN :start AND :end " +
            "ORDER BY ABS(DATEDIFF(SECOND, recorded_at, :target))",
            nativeQuery = true)
    LocalDateTime findNearestSnapshotTime(
            @Param("target") LocalDateTime target,
            @Param("start")  LocalDateTime start,
            @Param("end")    LocalDateTime end);

    /**
     * Sums occupancy across all levels at an EXACT timestamp (±1 second).
     */
    @Query("SELECT SUM(o.occupancy) FROM OccupancyLog o " +
            "WHERE o.recordedAt BETWEEN :start AND :end")
    Integer sumOccupancyByRecordedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end")   LocalDateTime end);



}