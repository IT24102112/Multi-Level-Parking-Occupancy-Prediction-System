package com.parking.controller;

import com.parking.model.ParkingLog;
import com.parking.service.ParkingLogService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class ParkingLogController {

    private final ParkingLogService parkingLogService;

    public ParkingLogController(ParkingLogService parkingLogService) {
        this.parkingLogService = parkingLogService;
    }

    @GetMapping
    public List<ParkingLog> getAllLogs() {
        return parkingLogService.getAllLogs();
    }

    @PostMapping
    public ParkingLog saveLog(@RequestBody ParkingLog log) {
        if (log.getEventTime() == null) {
            log.setEventTime(LocalDateTime.now());
        }
        return parkingLogService.saveLog(log);
    }
}
