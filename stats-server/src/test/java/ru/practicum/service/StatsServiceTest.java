package ru.practicum.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ViewStats;
import ru.practicum.exception.BadRequestException;
import ru.practicum.model.EndpointHit;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private StatsRepository statsRepository;

    @InjectMocks
    private StatsService statsService;

    private EndpointHit endpointHit;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        startTime = LocalDateTime.now().minusDays(1);
        endTime = LocalDateTime.now();

        endpointHit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(startTime)
                .build();
    }

    @Test
    void saveHit_WhenValidHit_ShouldReturnSavedHit() {
        when(statsRepository.save(any(EndpointHit.class))).thenReturn(endpointHit);

        EndpointHit result = statsService.saveHit(endpointHit);

        assertNotNull(result);
        assertEquals("ewm-main-service", result.getApp());
        assertEquals("/events/1", result.getUri());
        verify(statsRepository, times(1)).save(endpointHit);
    }

    @Test
    void getStats_WhenNoUrisAndNotUnique_ShouldReturnAllStats() {
        List<ViewStats> expectedStats = List.of(
                new ViewStats("ewm-main-service", "/events/1", 10L),
                new ViewStats("ewm-main-service", "/events/2", 5L)
        );

        when(statsRepository.getStats(startTime, endTime, null))
                .thenReturn(expectedStats);

        List<ViewStats> result = statsService.getStats(startTime, endTime, null, false);

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getHits());
        verify(statsRepository, times(1)).getStats(startTime, endTime, null);
    }

    @Test
    void getStats_WhenWithUrisAndNotUnique_ShouldReturnFilteredStats() {
        List<String> uris = List.of("/events/1", "/events/2");
        List<ViewStats> expectedStats = List.of(
                new ViewStats("ewm-main-service", "/events/1", 8L)
        );

        when(statsRepository.getStats(startTime, endTime, uris))
                .thenReturn(expectedStats);

        List<ViewStats> result = statsService.getStats(startTime, endTime, uris, false);

        assertEquals(1, result.size());
        assertEquals("/events/1", result.get(0).getUri());
        verify(statsRepository, times(1)).getStats(startTime, endTime, uris);
    }

    @Test
    void getStats_WhenUniqueTrue_ShouldReturnUniqueStats() {
        List<String> uris = List.of("/events/1");
        List<ViewStats> expectedStats = List.of(
                new ViewStats("ewm-main-service", "/events/1", 3L)
        );

        when(statsRepository.getUniqueStats(startTime, endTime, uris))
                .thenReturn(expectedStats);

        List<ViewStats> result = statsService.getStats(startTime, endTime, uris, true);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getHits());
        verify(statsRepository, times(1)).getUniqueStats(startTime, endTime, uris);
    }

    @Test
    void getStats_WhenStartAfterEnd_ShouldThrowException() {
        LocalDateTime invalidStart = endTime.plusDays(1);

        assertThrows(BadRequestException.class, () ->
                statsService.getStats(invalidStart, endTime, null, false));
    }

    @Test
    void getStats_WhenEmptyUris_ShouldHandleCorrectly() {
        List<ViewStats> expectedStats = List.of();

        when(statsRepository.getStats(startTime, endTime, List.of()))
                .thenReturn(expectedStats);

        List<ViewStats> result = statsService.getStats(startTime, endTime, List.of(), false);

        assertTrue(result.isEmpty());
        verify(statsRepository, times(1)).getStats(startTime, endTime, List.of());
    }
}