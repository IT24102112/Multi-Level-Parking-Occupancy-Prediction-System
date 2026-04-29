package com.parking.model;

import javax.persistence.*;

@Entity
@Table(name = "parking_level")
public class ParkingLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "level_name", nullable = false, unique = true)
    private String levelName;

    @Column(name = "total_slots", nullable = false)
    private int totalSlots;

    @Column(name = "current_occupancy", nullable = false)
    private int currentOccupancy;

    // Constructors
    public ParkingLevel() {}

    public ParkingLevel(String levelName, int totalSlots, int currentOccupancy) {
        this.levelName = levelName;
        this.totalSlots = totalSlots;
        this.currentOccupancy = currentOccupancy;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }

    public int getTotalSlots() { return totalSlots; }
    public void setTotalSlots(int totalSlots) { this.totalSlots = totalSlots; }

    public int getCurrentOccupancy() { return currentOccupancy; }
    public void setCurrentOccupancy(int currentOccupancy) { this.currentOccupancy = currentOccupancy; }

    // Helper
    public int getAvailableSlots() {
        return totalSlots - currentOccupancy;
    }

    public String getOccupancyColor() {
        double ratio = (double) currentOccupancy / totalSlots;
        if (ratio < 0.5) return "#4CAF50";
        else if (ratio < 0.8) return "#FF9800";
        else return "#F44336";
    }
}