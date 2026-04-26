package com.parking.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor")
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "parking_level_id", nullable = false)
    private ParkingLevel parkingLevel;

    @Column(name = "sensor_type", nullable = false)
    private String sensorType; // "ENTRANCE" or "EXIT"

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    public Sensor() {}

    public Sensor(ParkingLevel parkingLevel, String sensorType, boolean isActive) {
        this.parkingLevel = parkingLevel;
        this.sensorType = sensorType;
        this.isActive = isActive;
        this.lastHeartbeat = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public ParkingLevel getParkingLevel() { return parkingLevel; }
    public void setParkingLevel(ParkingLevel parkingLevel) { this.parkingLevel = parkingLevel; }
    public String getSensorType() { return sensorType; }
    public void setSensorType(String sensorType) { this.sensorType = sensorType; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    // Check if sensor is online (heartbeat in last 30s)
    public boolean isOnline() {
        return lastHeartbeat != null && lastHeartbeat.isAfter(LocalDateTime.now().minusSeconds(30));
    }
}