package com.parking.service;

import com.parking.model.ParkingLevel;
import com.parking.repository.ParkingLevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingServiceTest {

    @Mock
    private ParkingLevelRepository parkingLevelRepository;

    @InjectMocks
    private ParkingService parkingService;

    private ParkingLevel level1, level2;

    @BeforeEach
    void setUp() {
        level1 = new ParkingLevel("Level 1", 300, 120);
        level2 = new ParkingLevel("Level 2", 300, 200);
    }

    @Test
    void getAllLevels_ReturnsAll() {
        when(parkingLevelRepository.findAll()).thenReturn(Arrays.asList(level1, level2));
        List<ParkingLevel> levels = parkingService.getAllLevels();
        assertEquals(2, levels.size());
    }

    @Test
    void getLevel_Existing_ReturnsLevel() {
        when(parkingLevelRepository.findByLevelName("Level 1")).thenReturn(Optional.of(level1));
        ParkingLevel found = parkingService.getLevel("Level 1");
        assertNotNull(found);
        assertEquals("Level 1", found.getLevelName());
    }

    @Test
    void getLevel_NonExisting_ReturnsNull() {
        when(parkingLevelRepository.findByLevelName("Unknown")).thenReturn(Optional.empty());
        assertNull(parkingService.getLevel("Unknown"));
    }

    @Test
    void updateOccupancy_ValidLevel_Updates() {
        when(parkingLevelRepository.findByLevelName("Level 1")).thenReturn(Optional.of(level1));
        when(parkingLevelRepository.save(any())).thenReturn(level1);

        parkingService.updateOccupancy("Level 1", 150);

        assertEquals(150, level1.getCurrentOccupancy());
        verify(parkingLevelRepository).save(level1);
    }

    @Test
    void updateOccupancy_InvalidLevel_DoesNothing() {
        when(parkingLevelRepository.findByLevelName("Unknown")).thenReturn(Optional.empty());
        parkingService.updateOccupancy("Unknown", 50);
        verify(parkingLevelRepository, never()).save(any());
    }
}