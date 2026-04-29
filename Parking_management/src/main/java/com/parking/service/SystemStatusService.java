package com.parking.service;

import com.parking.model.Sensor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SystemStatusService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SensorService sensorService;

    // Return list of actual sensors (with lastHeartbeat / isOnline available)
    public List<Sensor> getSensorStatuses() {
        // Delegate to SensorService which reads from the repository
        return sensorService.getAllSensors();
    }

    public String getDatabaseStatus() {
        try {
            jdbcTemplate.execute("SELECT 1");
            return "Connected";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public String getModelStatus() {
        return "Online (simulated)";
    }

    public LocalDateTime getLastChecked() {
        return LocalDateTime.now();
    }
}