package com.parking.repository;

import com.parking.model.OccupancyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}