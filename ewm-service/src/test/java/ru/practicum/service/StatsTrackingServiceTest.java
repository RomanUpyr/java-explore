package ru.practicum.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import ru.practicum.StatsClient;
import ru.practicum.EndpointHitRequest;
import ru.practicum.ViewStats;
import ru.practicum.model.Event;
import ru.practicum.repository.EventRepository;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsTrackingServiceTest {

    @Mock
    private StatsClient statsClient;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TaskExecutor taskExecutor;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private StatsTrackingService statsTrackingService;

    @Test
    void trackHit_WithValidData_ShouldSendHit() {
        // Given
        String uri = "/events/1";
        String clientIp = "192.168.1.1";
        String appName = "test-app";

        when(statsClient.isServiceAvailable()).thenReturn(true);
        doNothing().when(statsClient).saveHit(any(EndpointHitRequest.class));

        // When
        statsTrackingService.trackHit(uri, clientIp, appName);

        // Then
        verify(statsClient).saveHit(any(EndpointHitRequest.class));
    }

    @Test
    void trackHit_WhenServiceUnavailable_ShouldNotSendHit() {
        // Given
        String uri = "/events/1";
        String clientIp = "192.168.1.1";

        when(statsClient.isServiceAvailable()).thenReturn(false);

        // When
        statsTrackingService.trackHit(uri, clientIp);

        // Then
        verify(statsClient, never()).saveHit(any(EndpointHitRequest.class));
    }

    @Test
    void trackHit_WithRequest_ShouldExtractUriAndIp() {
        // Given
        String uri = "/events/1";
        String clientIp = "192.168.1.1";

        when(request.getRequestURI()).thenReturn(uri);
        when(statsClient.isServiceAvailable()).thenReturn(true);
        doNothing().when(statsClient).saveHit(any(EndpointHitRequest.class));

        // When
        statsTrackingService.trackHit(request);

        // Then
        verify(statsClient).saveHit(any(EndpointHitRequest.class));
    }

    @Test
    void getClientIp_WithXForwardedFor_ShouldReturnFirstIp() {
        // Given
        String xForwardedFor = "192.168.1.1, 10.0.0.1";
        when(request.getHeader("X-Forwarded-For")).thenReturn(xForwardedFor);

        // When
        String result = statsTrackingService.getClientIp(request);

        // Then
        assertEquals("192.168.1.1", result);
    }

    @Test
    void getClientIp_WithXRealIp_ShouldReturnRealIp() {
        // Given
        String xRealIp = "192.168.1.2";
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(xRealIp);

        // When
        String result = statsTrackingService.getClientIp(request);

        // Then
        assertEquals("192.168.1.2", result);
    }

    @Test
    void getClientIp_WithRemoteAddr_ShouldReturnRemoteAddr() {
        // Given
        String remoteAddr = "192.168.1.3";
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);

        // When
        String result = statsTrackingService.getClientIp(request);

        // Then
        assertEquals("192.168.1.3", result);
    }

    @Test
    void updateEventViews_WhenEventExists_ShouldUpdateViews() {
        // Given
        Long eventId = 1L;
        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .views(0L)
                .build();

        ViewStats viewStats = new ViewStats("app", "/events/1", 100L);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(statsClient.getStats(any(LocalDateTime.class), any(LocalDateTime.class), anyList(), eq(true)))
                .thenReturn(List.of(viewStats));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // When
        statsTrackingService.updateEventViews(eventId);

        // Then
        verify(eventRepository).save(event);
        assertEquals(100L, event.getViews());
    }

    @Test
    void updateEventViews_WhenNoStats_ShouldSetZeroViews() {
        // Given
        Long eventId = 1L;
        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .views(50L)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(statsClient.getStats(any(LocalDateTime.class), any(LocalDateTime.class), anyList(), eq(true)))
                .thenReturn(List.of());
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // When
        statsTrackingService.updateEventViews(eventId);

        // Then
        verify(eventRepository).save(event);
        assertEquals(0L, event.getViews());
    }

    @Test
    void updateEventViewsAsync_ShouldExecuteTask() {
        // Given
        Long eventId = 1L;

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));

        // When
        statsTrackingService.updateEventViewsAsync(eventId);

        // Then
        verify(taskExecutor).execute(any(Runnable.class));
    }

    @Test
    void formatDateTime_ShouldFormatCorrectly() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 31, 12, 0, 0);

        // When
        String result = statsTrackingService.formatDateTime(dateTime);

        // Then
        assertEquals("2024-12-31 12:00:00", result);
    }

    @Test
    void parseDateTime_ShouldParseCorrectly() {
        // Given
        String dateTimeStr = "2024-12-31 12:00:00";

        // When
        LocalDateTime result = statsTrackingService.parseDateTime(dateTimeStr);

        // Then
        assertNotNull(result);
        assertEquals(2024, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(31, result.getDayOfMonth());
    }

    @Test
    void parseDateTime_WithNull_ShouldReturnNull() {
        // When
        LocalDateTime result = statsTrackingService.parseDateTime(null);

        // Then
        assertNull(result);
    }

    @Test
    void getEventViews_WhenEventExists_ShouldReturnViews() {
        // Given
        Long eventId = 1L;
        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .views(150L)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When
        int result = statsTrackingService.getEventViews(eventId);

        // Then
        assertEquals(150, result);
    }

}