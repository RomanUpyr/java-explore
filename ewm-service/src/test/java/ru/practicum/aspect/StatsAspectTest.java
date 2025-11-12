package ru.practicum.aspect;

import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.service.BaseService;
import ru.practicum.service.StatsTrackingService;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsAspectTest {

    @Mock
    private StatsTrackingService statsTrackingService;

    @Mock
    private BaseService baseService;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private StatsAspect statsAspect;

    @Test
    void saveHitAfterSuccessfulRequest_WithEventDetails_ShouldTrackHitAndUpdateViews() {
        // Given
        String uri = "/events/123";
        String clientIp = "192.168.1.1";

        when(request.getRequestURI()).thenReturn(uri);
        when(baseService.getClientIp(request)).thenReturn(clientIp);
        doNothing().when(statsTrackingService).trackHit(request);
        doNothing().when(statsTrackingService).updateEventViewsAsync(123L);

        // When
        statsAspect.saveHitAfterSuccessfulRequest(joinPoint, request);

        // Then
        verify(statsTrackingService).trackHit(request);
        verify(statsTrackingService).updateEventViewsAsync(123L);
    }

    @Test
    void saveHitAfterSuccessfulRequest_WithEventsList_ShouldOnlyTrackHit() {
        // Given
        String uri = "/events";
        String clientIp = "192.168.1.1";

        when(request.getRequestURI()).thenReturn(uri);
        when(baseService.getClientIp(request)).thenReturn(clientIp);
        doNothing().when(statsTrackingService).trackHit(request);

        // When
        statsAspect.saveHitAfterSuccessfulRequest(joinPoint, request);

        // Then
        verify(statsTrackingService).trackHit(request);
        verify(statsTrackingService, never()).updateEventViewsAsync(any());
    }

    @Test
    void saveHitAfterSuccessfulRequest_WithInvalidEventId_ShouldHandleGracefully() {
        // Given
        String uri = "/events/invalid";
        String clientIp = "192.168.1.1";

        when(request.getRequestURI()).thenReturn(uri);
        when(baseService.getClientIp(request)).thenReturn(clientIp);
        doNothing().when(statsTrackingService).trackHit(request);

        // When
        statsAspect.saveHitAfterSuccessfulRequest(joinPoint, request);

        // Then
        verify(statsTrackingService).trackHit(request);
        verify(statsTrackingService, never()).updateEventViewsAsync(any());
    }

    @Test
    void saveHitAfterSuccessfulRequest_WhenExceptionOccurs_ShouldLogError() {
        // Given
        String uri = "/events/123";
        String clientIp = "192.168.1.1";

        when(request.getRequestURI()).thenReturn(uri);
        when(baseService.getClientIp(request)).thenReturn(clientIp);
        doThrow(new RuntimeException("Test exception")).when(statsTrackingService).trackHit(request);

        // When
        statsAspect.saveHitAfterSuccessfulRequest(joinPoint, request);

        // Then - Should not throw exception, just log error
        verify(statsTrackingService).trackHit(request);
    }

    @Test
    void extractEventIdFromUri_WithValidUri_ShouldReturnEventId() {
        // Given
        StatsAspect aspect = new StatsAspect(statsTrackingService, baseService);
        String uri = "/events/123";

        // When
        Long eventId = aspect.extractEventIdFromUri(uri);

        // Then
        assertEquals(123L, eventId);
    }

    @Test
    void extractEventIdFromUri_WithInvalidUri_ShouldReturnNull() {
        // Given
        StatsAspect aspect = new StatsAspect(statsTrackingService, baseService);
        String uri = "/events/invalid";

        // When
        Long eventId = aspect.extractEventIdFromUri(uri);

        // Then
        assertNull(eventId);
    }

    @Test
    void extractEventIdFromUri_WithNestedUri_ShouldReturnEventId() {
        // Given
        StatsAspect aspect = new StatsAspect(statsTrackingService, baseService);
        String uri = "/api/v1/events/456/details";

        // When
        Long eventId = aspect.extractEventIdFromUri(uri);

        // Then
        assertEquals(456L, eventId);
    }
}