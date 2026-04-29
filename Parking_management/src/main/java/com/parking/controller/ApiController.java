package com.parking.controller;


import com.parking.model.ParkingLevel;
import com.parking.model.Sensor;

import com.parking.service.ParkingService;
import com.parking.service.PredictionService;
import com.parking.service.SensorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    private ParkingService parkingService;

    @Autowired
    private SensorService sensorService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private PredictionService predictionService;

    @GetMapping("/occupancy")
    public List<ParkingLevel> getOccupancy() {
        return parkingService.getAllLevels();
    }

    @GetMapping("/predict")
    public List<Integer> getAIPrediction() {
        return predictionService.getPredictions();
    }

    // ---------------- ESP32 UPDATE ENDPOINT ----------------
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateFromSensor(
            @RequestBody Map<String, String> payload) {

        String levelName = payload.get("levelName");
        String entryExit = payload.get("entryExit");

        Map<String, Object> response = new HashMap<>();

        if (levelName == null || entryExit == null) {
            response.put("status", "error");
            response.put("message", "levelName and entryExit are required");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            logger.info("Processing sensor event - Level: {}, Direction: {}", levelName, entryExit);

            ParkingLevel updatedLevel =
                    parkingService.processSensorEvent(levelName, entryExit);

            int total = updatedLevel.getTotalSlots();
            int current = updatedLevel.getCurrentOccupancy();

            response.put("status", "success");
            response.put("levelName", updatedLevel.getLevelName());
            response.put("currentOccupancy", current);
            response.put("totalSlots", total);
            response.put("availableSlots", total - current);
            response.put("isFull", current >= total);

            // Broadcast the update to all WebSocket clients
            logger.info("Broadcasting occupancy update via WebSocket: {}", response);
            messagingTemplate.convertAndSend("/topic/occupancy", response);
            logger.info("✓ WebSocket message sent successfully");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("Error processing sensor event", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ESP32 SENSOR STATUS UPDATE ENDPOINT
    @PostMapping("/sensor/update")
    public ResponseEntity<Map<String, Object>> updateSensorStatus(
            @RequestBody Map<String, Object> payload) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long sensorId = Long.valueOf(payload.get("sensorId").toString());
            Boolean isActive = (Boolean) payload.get("isActive");

            logger.info("Updating sensor {} status to: {}", sensorId, isActive);

            Sensor updatedSensor = sensorService.updateSensorStatus(sensorId, isActive);

            if (updatedSensor == null) {
                response.put("status", "error");
                response.put("message", "Sensor not found");
                return ResponseEntity.badRequest().body(response);
            }

            response.put("status", "success");
            response.put("sensorId", updatedSensor.getId());
            response.put("levelName", updatedSensor.getParkingLevel().getLevelName());
            response.put("sensorType", updatedSensor.getSensorType());
            response.put("isActive", updatedSensor.isActive());

            logger.info("✓ Sensor {} status updated successfully", sensorId);

            // Broadcast sensor status update to WebSocket clients
            messagingTemplate.convertAndSend("/topic/sensors", response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating sensor status", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Get all active sensors
    @GetMapping("/sensors/active")
    public ResponseEntity<List<Sensor>> getActiveSensors() {
        List<Sensor> activeSensors = sensorService.getActiveSensors();
        return ResponseEntity.ok(activeSensors);
    }

    // Get all sensors
    @GetMapping("/sensors")
    public ResponseEntity<List<Sensor>> getAllSensors() {
        List<Sensor> allSensors = sensorService.getAllSensors();
        return ResponseEntity.ok(allSensors);
    }

    // Get sensors for a specific level
    @GetMapping("/sensors/level/{levelId}")
    public ResponseEntity<List<Sensor>> getSensorsByLevel(@PathVariable Long levelId) {
        List<Sensor> sensors = sensorService.getSensorsByLevel(levelId);
        return ResponseEntity.ok(sensors);
    }

    @PostMapping("/sensor/heartbeat")
    public ResponseEntity<Map<String, Object>> receiveHeartbeat(
            @RequestBody Map<String, Object> payload) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (payload.get("sensorId") == null) {
                response.put("status", "error");
                response.put("message", "sensorId is required");
                return ResponseEntity.badRequest().body(response);
            }

            Long sensorId = Long.valueOf(payload.get("sensorId").toString());

            logger.debug("Received heartbeat from sensor {}", sensorId);

            Sensor sensor = sensorService.recordHeartbeat(sensorId);

            if (sensor == null) {
                response.put("status", "error");
                response.put("message", "Sensor not found");
                return ResponseEntity.badRequest().body(response);
            }

            // Build response
            response.put("status", "success");
            response.put("sensorId", sensor.getId());
            response.put("levelName", sensor.getParkingLevel().getLevelName());
            response.put("sensorType", sensor.getSensorType());
            response.put("isActive", sensor.isActive());
            response.put("lastHeartbeat", sensor.getLastHeartbeat());
            response.put("isOnline", sensor.isOnline());

            // Broadcast to WebSocket clients
            messagingTemplate.convertAndSend("/topic/sensors", response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing heartbeat", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

    }
}