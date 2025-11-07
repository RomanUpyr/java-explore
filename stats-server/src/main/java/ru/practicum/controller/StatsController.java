package ru.practicum.controller;

import ru.practicum.EndpointHitRequest;
import ru.practicum.exception.BadRequestException;
import ru.practicum.model.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.repository.StatsRepository;
import ru.practicum.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST контроллер для обработки HTTP запросов статистики.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class StatsController {
    private final StatsRepository statsRepository;

    /**
     * Сервис для бизнес-логики статистики.
     */
    private final StatsService statsService;

    /**
     * Обрабатывает запрос на сохранение информации о посещении эндпоинта.
     */
    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointHit hit(@Valid @RequestBody EndpointHitRequest hitRequest) {
        log.debug("Получен POST /hit запрос: {}", hitRequest);

        // Парсим timestamp из String в LocalDateTime
        LocalDateTime timestamp = parseDateTime(hitRequest.getTimestamp());

        // Создаем Entity
        EndpointHit hit = EndpointHit.builder()
                .app(hitRequest.getApp())
                .uri(hitRequest.getUri())
                .ip(hitRequest.getIp())
                .timestamp(timestamp)
                .build();

        EndpointHit savedHit = statsService.saveHit(hit);
        log.debug("Сохранена информация о посещении: {}", savedHit);
        return savedHit;
    }

    /**
     * Обрабатывает запрос на получение статистики по посещениям.
     */
    @GetMapping("/stats")
    public List<ViewStats> getStats(
            // Парсим вручную из-за тестов
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {

        LocalDateTime startTime = parseDateTime(start);
        LocalDateTime endTime = parseDateTime(end);

        log.debug("=== STATS REQUEST DETAILS ===");
        log.debug("Start: {}, End: {}", startTime, endTime);
        log.debug("URIs: {}", uris);
        log.debug("Unique: {}", unique);

        log.debug("Получен GET /stats запрос: start={}, end={}, uris={}, unique={}",
                startTime, endTime, uris, unique);

        validateTimeRange(startTime, endTime);

        // Проверим сколько всего записей в БД
        long totalHits = statsRepository.count();
        log.debug("TOTAL HITS IN DB: {}", totalHits);

        if (uris != null) {
            uris = uris.stream()
                    .map(uri -> URLDecoder.decode(uri, StandardCharsets.UTF_8))
                    .toList();
            log.debug("Декодированные URIs: {}", uris);
        }

        List<ViewStats> stats = statsService.getStats(startTime, endTime, uris, unique);

        log.debug("Возвращено {} записей статистики", stats.size());

        return stats;
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        return LocalDateTime.parse(dateTimeStr.replace(" ", "T"));
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new BadRequestException("Начальная дата не может быть позже конечной");
        }
    }
}