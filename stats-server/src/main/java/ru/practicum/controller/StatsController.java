package ru.practicum.controller;

import ru.practicum.EndpointHitRequest;
import ru.practicum.model.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /**
     * Сервис для бизнес-логики статистики.
     */
    private final StatsService statsService;

    /**
     * Обрабатывает запрос на сохранение информации о посещении эндпоинта.
     */
    @PostMapping("/hit")
    public ResponseEntity<EndpointHit> hit(@Valid @RequestBody EndpointHitRequest hitRequest) {
        log.debug("Получен POST /hit запрос: {}", hitRequest);
        EndpointHit savedHit = statsService.saveHit(hitRequest);
        return new ResponseEntity<>(savedHit, HttpStatus.CREATED);
    }

    /**
     * Обрабатывает запрос на получение статистики по посещениям.
     */
    @GetMapping("/stats")
    public ResponseEntity<List<ViewStats>> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {

        log.debug("Получен GET /stats запрос: start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);

        if (uris != null) {
            uris = uris.stream()
                    .map(uri -> URLDecoder.decode(uri, StandardCharsets.UTF_8))
                    .toList();
            log.debug("Декодированные URIs: {}", uris);
        }

        List<ViewStats> stats = statsService.getStats(start, end, uris, unique);

        return new ResponseEntity<>(stats, HttpStatus.OK);
    }
}