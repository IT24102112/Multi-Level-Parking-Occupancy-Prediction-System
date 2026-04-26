package com.parking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SystemStatusService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Inner class to represent a sensor status
    public static class SensorStatus {
        private String level;
        private String type; // "Entrance" or "Exit"
        private String status;

        public SensorStatus(String level, String type, String status) {
            this.level = level;
            this.type = type;
            this.status = status;
        }

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // Return list of sensor statuses (entrance and exit for each level)
    public List<SensorStatus> getSensorStatuses() {
        List<SensorStatus> statuses = new ArrayList<>();
        String[] levels = {"Level 1", "Level 2", "Level 3", "Level 4"};
        for (String level : levels) {
            statuses.add(new SensorStatus(level, "Entrance", "OK"));
            statuses.add(new SensorStatus(level, "Exit", "OK"));
        }
        return statuses;
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