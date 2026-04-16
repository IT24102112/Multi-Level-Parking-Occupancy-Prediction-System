package com.parking.service;

import com.parking.model.ParkingLog;
import com.parking.repository.ParkingLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingLogService {

    private final ParkingLogRepository parkingLogRepository;

    public ParkingLogService(ParkingLogRepository parkingLogRepository) {
        this.parkingLogRepository = parkingLogRepository;
    }

    public List<ParkingLog> getAllLogs() {
        return parkingLogRepository.findAll();
    }

    public ParkingLog saveLog(ParkingLog log) {
        return parkingLogRepository.save(log);
    }
}
