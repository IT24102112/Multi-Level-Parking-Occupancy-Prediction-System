package com.parking.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "occupancy_log")
public class OccupancyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "level_name", nullable = false)
    private String levelName;

    @Column(nullable = false)
    private int occupancy;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    // Constructors
    public OccupancyLog() {}

    public OccupancyLog(String levelName, int occupancy, LocalDateTime recordedAt) {
        this.levelName = levelName;
        this.occupancy = occupancy;
        this.recordedAt = recordedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }

    public int getOccupancy() { return occupancy; }
    public void setOccupancy(int occupancy) { this.occupancy = occupancy; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}