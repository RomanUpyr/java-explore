package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.EndpointHitRequest;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ViewStats;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.Event;
import ru.practicum.repository.EventRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsTrackingService {
    private final StatsClient statsClient;
    private final EventRepository eventRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_APP_NAME = "ewm-main-service";

    /**
     * Сохраняет информацию о посещении эндпоинта
     */
    public void trackHit(String uri, String clientIp) {
        trackHit(uri, clientIp, DEFAULT_APP_NAME);
    }

    /**
     * Сохраняет информацию о посещении эндпоинта с указанием имени приложения
     */
    public void trackHit(String uri, String clientIp, String appName) {
        try {
            if (!statsClient.isServiceAvailable()) {
                log.warn("Stats server is unavailable, skipping hit tracking for: {}", uri);
                return;
            }

            EndpointHitRequest hitRequest = EndpointHitRequest.builder()
                    .app(appName)
                    .uri(uri)
                    .ip(clientIp)
                    .timestamp(LocalDateTime.now().format(FORMATTER))
                    .build();

            statsClient.saveHit(hitRequest);
            log.debug("Статистика отправлена для URI: {}, IP: {}, App: {}", uri, clientIp, appName);

        } catch (Exception e) {
            log.error("Ошибка при отправке статистики для URI: {}", uri, e);
        }
    }

    /**
     * Сохраняет информацию о посещении эндпоинта на основе HttpServletRequest
     */
    public void trackHit(HttpServletRequest request) {
        trackHit(request, DEFAULT_APP_NAME);
    }

    /**
     * Сохраняет информацию о посещении эндпоинта на основе HttpServletRequest с указанием имени приложения
     */
    public void trackHit(HttpServletRequest request, String appName) {
        String uri = request.getRequestURI();
        String clientIp = getClientIp(request);
        trackHit(uri, clientIp, appName);
    }

    /**
     * Получает реальный IP адрес клиента с учетом прокси
     */
    public String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Форматирует LocalDateTime в строку
     */
    public String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(FORMATTER);
    }

    /**
     * Парсит строку в LocalDateTime
     */
    public LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr.replace(" ", "T"));
    }

    @Transactional
    public void updateEventViews(Long eventId, String clientIp) {
        try {

            trackHit("/events/" + eventId, clientIp);
            Thread.sleep(100);

            // Получаем статистику просмотров для конкретного события
            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now();
            List<String> uris = List.of("/events/" + eventId);

            List<ViewStats> stats = statsClient.getStats(start, end, uris, true);

            Long uniqueViews = 0L;
            if (!stats.isEmpty()) {
                uniqueViews = stats.get(0).getHits();
            }

            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));

            event.setViews(uniqueViews);
            eventRepository.save(event);

            log.debug("Updated views for event id={}: {}", eventId, uniqueViews);

        } catch (Exception e) {
            log.error("Failed to update views for event id={}", eventId, e);
        }
    }

    /**
     * Получает количество просмотров события (без обновления)
     */
    public int getEventViews(Long eventId) {
        try {
            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now();
            List<String> uris = List.of("/events/" + eventId);

            List<ViewStats> stats = statsClient.getStats(start, end, uris, true);

            if (!stats.isEmpty()) {
                return Math.toIntExact(stats.get(0).getHits());
            }
            return 0;

        } catch (Exception e) {
            log.error("Failed to get views for event id={}", eventId, e);
            return 0;
        }
    }
}
