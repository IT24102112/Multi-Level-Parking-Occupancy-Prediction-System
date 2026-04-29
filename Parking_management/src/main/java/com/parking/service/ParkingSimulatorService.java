package com.parking.service;

import com.parking.config.SimulatorConfig;
import com.parking.model.ParkingLevel;
import com.parking.model.ParkingLog;
import com.parking.repository.ParkingLevelRepository;
import com.parking.repository.ParkingLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simulates IoT sensor events based on Kandy CBD parking research.
 *
 * Key design decisions:
 *  - ALWAYS fires at least MIN_EVENTS_PER_TICK entries AND exits every tick
 *    so parking_logs always has fresh activity and reports always have today's data
 *  - Target occupancy guides the NET direction (more entries or more exits)
 *    but both directions always happen — realistic car park behaviour
 *  - Resets parking_level to time-appropriate values when simulator starts
 *  - Runs every 30 seconds
 */
@Service
public class ParkingSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(ParkingSimulatorService.class);

    private static final int LEVEL_CAPACITY  = 300;
    private static final int TOTAL_CAPACITY  = 1200;

    // Minimum events fired EACH direction per tick regardless of occupancy
    // This ensures parking_logs always has today's data for reports
    private static final int MIN_EVENTS_PER_TICK = 3;

    // ── Hourly occupancy targets (total across all 4 levels) ─────────────
    // Based on Kandy CBD research — peak 10am–3pm, near-zero overnight
    private static final int[] HOURLY_TARGET_WEEKDAY = {
            //  00   01   02   03   04   05   06   07   08   09
            30,  25,  20,  18,  18,  25,  60, 180, 420, 680,
            //  10   11   12   13   14   15   16   17   18   19
            900, 1050,1150,1180,1150,1050, 880, 700, 500, 320,
            //  20   21   22   23
            180,  90,  50,  35
    };

    private static final int[] HOURLY_TARGET_WEEKEND = {
            //  00   01   02   03   04   05   06   07   08   09
            35,  28,  22,  20,  20,  28,  65, 150, 320, 580,
            //  10   11   12   13   14   15   16   17   18   19
            850, 1000,1150,1200,1200,1150,1000, 850, 650, 430,
            //  20   21   22   23
            250, 140,  70,  40
    };

    @Autowired private SimulatorConfig        config;
    @Autowired private ParkingLevelRepository parkingLevelRepository;
    @Autowired private ParkingLogRepository   parkingLogRepository;

    private final AtomicBoolean running      = new AtomicBoolean(true);
    private volatile String     statusMessage = "Simulator initialising...";
    private volatile LocalDateTime lastTick;
    private volatile long       pauseUntil   = 0;

    // ── Main tick — every 30 seconds ──────────────────────────────────────
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void tick() {
        if (!config.isEnabled() || !running.get()) return;
        if (System.currentTimeMillis() < pauseUntil) {
            statusMessage = "Paused — real sensor active";
            return;
        }

        try {
            LocalDateTime now    = LocalDateTime.now();
            int target           = getTargetForNow();
            int noise            = (int)(target * 0.03 * (Math.random() * 2 - 1));
            int adjustedTarget   = Math.max(0, Math.min(TOTAL_CAPACITY, target + noise));

            List<ParkingLevel> levels = parkingLevelRepository.findAll();
            int currentTotal = levels.stream()
                    .mapToInt(ParkingLevel::getCurrentOccupancy).sum();

            int diff = adjustedTarget - currentTotal;

            // ── Always fire MIN_EVENTS_PER_TICK in BOTH directions ────────
            // This ensures parking_logs always gets today's entries AND exits
            // Extra events fire in whichever direction moves toward target
            int extraEntries = 0;
            int extraExits   = 0;

            if (diff > 5) {
                // Need more vehicles — fire extra entries
                extraEntries = Math.min(15, diff / 4);
            } else if (diff < -5) {
                // Too many vehicles — fire extra exits
                extraExits = Math.min(15, Math.abs(diff) / 4);
            }

            // Fire exits first (some people leave)
            int totalExits = MIN_EVENTS_PER_TICK + extraExits;
            fireExits(levels, totalExits);

            // Then fire entries (new vehicles arriving)
            int totalEntries = MIN_EVENTS_PER_TICK + extraEntries;
            fireEntries(levels, totalEntries);

            // Refresh totals after events
            levels = parkingLevelRepository.findAll();
            int newTotal = levels.stream()
                    .mapToInt(ParkingLevel::getCurrentOccupancy).sum();

            String timeLabel = getTimeLabel(now.getHour());
            statusMessage = String.format(
                    "Running — %d vehicles | target %d | +%d -%d | %s",
                    newTotal, adjustedTarget, totalEntries, totalExits, timeLabel);

            lastTick = now;

        } catch (Exception e) {
            log.error("Simulator tick error: {}", e.getMessage(), e);
            statusMessage = "Error: " + e.getMessage();
        }
    }

    // ── Fire entry events ─────────────────────────────────────────────────
    private void fireEntries(List<ParkingLevel> levels, int count) {
        for (int i = 0; i < count; i++) {
            ParkingLevel level = pickLevelForEntry(levels);
            if (level == null) break;
            level.setCurrentOccupancy(level.getCurrentOccupancy() + 1);
            parkingLevelRepository.save(level);
            saveLog("IN", level.getLevelName());
        }
    }

    // ── Fire exit events ──────────────────────────────────────────────────
    private void fireExits(List<ParkingLevel> levels, int count) {
        for (int i = 0; i < count; i++) {
            ParkingLevel level = pickLevelForExit(levels);
            if (level == null) break;
            level.setCurrentOccupancy(level.getCurrentOccupancy() - 1);
            parkingLevelRepository.save(level);
            saveLog("OUT", level.getLevelName());
        }
    }

    // ── Level selection ───────────────────────────────────────────────────

    /** Entry: prefer Level 1 → 2 → 3 → 4 (ground fills first) */
    private ParkingLevel pickLevelForEntry(List<ParkingLevel> levels) {
        return levels.stream()
                .sorted(Comparator.comparing(ParkingLevel::getLevelName))
                .filter(l -> l.getCurrentOccupancy() < LEVEL_CAPACITY)
                .findFirst().orElse(null);
    }

    /** Exit: prefer Level 4 → 3 → 2 → 1 (top empties first) */
    private ParkingLevel pickLevelForExit(List<ParkingLevel> levels) {
        return levels.stream()
                .sorted(Comparator.comparing(ParkingLevel::getLevelName).reversed())
                .filter(l -> l.getCurrentOccupancy() > 0)
                .findFirst().orElse(null);
    }

    // ── Save parking log ──────────────────────────────────────────────────
    private void saveLog(String direction, String levelName) {
        int floor;
        if ("Level 2".equals(levelName)) {
            floor = 2;
        } else if ("Level 3".equals(levelName)) {
            floor = 3;
        } else if ("Level 4".equals(levelName)) {
            floor = 4;
        } else {
            floor = 1;
        }
        ParkingLog entry = new ParkingLog();
        entry.setDirection(direction);
        entry.setFloorNumber(floor);
        entry.setEventTime(LocalDateTime.now());
        parkingLogRepository.save(entry);
    }

    // ── Reset levels to a realistic distribution at startup/restart ───────
    @Transactional
    public void resetLevelsToTarget(int total) {
        List<ParkingLevel> levels = parkingLevelRepository.findAll();
        if (levels.isEmpty()) return;

        int[] dist = distributeFill(total);
        levels.sort(Comparator.comparing(ParkingLevel::getLevelName));

        for (int i = 0; i < Math.min(levels.size(), dist.length); i++) {
            levels.get(i).setCurrentOccupancy(dist[i]);
            parkingLevelRepository.save(levels.get(i));
        }
        log.info("Levels reset — distribution: L1={} L2={} L3={} L4={}",
                dist[0], dist[1], dist[2], dist[3]);
    }

    private int[] distributeFill(int total) {
        double ratio = (double) total / TOTAL_CAPACITY;
        int l1, l2, l3, l4;
        Random rnd = new Random();

        if (ratio <= 0.0) {
            return new int[]{0, 0, 0, 0};
        } else if (ratio < 0.25) {
            l1 = Math.min(LEVEL_CAPACITY, total);
            l2 = Math.max(0, total - l1);
            l3 = 0; l4 = 0;
        } else if (ratio < 0.50) {
            l1 = Math.min(LEVEL_CAPACITY, (int)(total * 0.60));
            l2 = Math.min(LEVEL_CAPACITY, (int)(total * 0.30));
            l3 = Math.max(0, total - l1 - l2);
            l4 = 0;
        } else if (ratio < 0.75) {
            l1 = Math.min(LEVEL_CAPACITY, (int)(total * 0.35));
            l2 = Math.min(LEVEL_CAPACITY, (int)(total * 0.30));
            l3 = Math.min(LEVEL_CAPACITY, (int)(total * 0.22));
            l4 = Math.max(0, total - l1 - l2 - l3);
        } else {
            l1 = Math.min(LEVEL_CAPACITY, (int)(total * 0.27));
            l2 = Math.min(LEVEL_CAPACITY, (int)(total * 0.26));
            l3 = Math.min(LEVEL_CAPACITY, (int)(total * 0.25));
            l4 = Math.max(0, Math.min(LEVEL_CAPACITY, total - l1 - l2 - l3));
        }

        // Small noise
        l1 = clamp(l1 + rnd.nextInt(5) - 2);
        l2 = clamp(l2 + rnd.nextInt(5) - 2);
        l3 = clamp(l3 + rnd.nextInt(5) - 2);
        l4 = clamp(l4 + rnd.nextInt(5) - 2);

        return new int[]{l1, l2, l3, l4};
    }

    private int clamp(int val) {
        return Math.max(0, Math.min(LEVEL_CAPACITY, val));
    }

    // ── Time helpers ──────────────────────────────────────────────────────

    public int getTargetForNow() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int dow  = now.getDayOfWeek().getValue();
        boolean isWeekend = (dow == 6 || dow == 7);
        return isWeekend
                ? HOURLY_TARGET_WEEKEND[hour]
                : HOURLY_TARGET_WEEKDAY[hour];
    }

    private String getTimeLabel(int hour) {
        if (hour >= 10 && hour <= 14) return "🔴 PEAK HOURS";
        if (hour >= 8  && hour <= 9)  return "🟡 Morning ramp-up";
        if (hour >= 15 && hour <= 18) return "🟡 Afternoon decline";
        if (hour >= 19 && hour <= 22) return "🟢 Evening";
        return "🟢 Off-peak";
    }

    // ── Public controls ───────────────────────────────────────────────────

    public void start() {
        running.set(true);
        int target = getTargetForNow();
        resetLevelsToTarget(target);
        statusMessage = "Simulator started — occupancy reset to "
                + target + " vehicles for current hour.";
        log.info("Parking simulator started. Target: {}", target);
    }

    public void stop() {
        running.set(false);
        statusMessage = "Simulator stopped manually.";
        log.info("Parking simulator stopped.");
    }

    public void pauseTemporarily(int seconds) {
        pauseUntil = System.currentTimeMillis() + (seconds * 1000L);
        log.info("Simulator paused for {} seconds.", seconds);
    }

    public boolean isRunning()         { return config.isEnabled() && running.get(); }
    public String getStatusMessage()   { return statusMessage; }
    public LocalDateTime getLastTick() { return lastTick; }
    public int getCurrentTarget()      { return getTargetForNow(); }
}