package com.parking.service;

import com.parking.model.Event;
import com.parking.repository.EventRepository;
import com.parking.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private Event testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setTitle("Test Event");
        testEvent.setStartDate(LocalDateTime.now().plusDays(1));
        testEvent.setEndDate(LocalDateTime.now().plusDays(1).plusHours(2));
    }

    @Test
    void saveEvent_ValidEvent_ReturnsSavedEvent() {
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        Event saved = eventService.saveEvent(testEvent);

        assertNotNull(saved);
        assertEquals("Test Event", saved.getTitle());
        verify(eventRepository, times(1)).save(testEvent);
    }

    @Test
    void getAllEvents_ReturnsListOfEvents() {
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findAll()).thenReturn(events);

        List<Event> result = eventService.getAllEvents();

        assertEquals(1, result.size());
        verify(eventRepository, times(1)).findAll();
    }

    @Test
    void getEvent_ValidId_ReturnsEvent() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        Event result = eventService.getEvent(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getEvent_InvalidId_ReturnsNull() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        Event result = eventService.getEvent(999L);

        assertNull(result);
    }

    @Test
    void deleteEvent_CallsRepositoryDelete() {
        doNothing().when(eventRepository).deleteById(1L);

        eventService.deleteEvent(1L);

        verify(eventRepository, times(1)).deleteById(1L);
    }
}