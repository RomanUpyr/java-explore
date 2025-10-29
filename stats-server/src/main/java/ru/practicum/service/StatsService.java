package ru.practicum.service;

import org.springframework.transaction.annotation.Transactional;
import ru.practicum.model.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервисный слой для бизнес-логики работы со статистикой.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    /**
     * Репозиторий для работы с базой данных.
     */
    private final StatsRepository statsRepository;

    /**
     * Сохраняет информацию о посещении эндпоинта.
     */
    @Transactional
    public EndpointHit saveHit(EndpointHit hit) {
        return statsRepository.save(hit);
    }

    /**
     * Получает статистику по посещениям за указанный период.
     */
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                                    List<String> uris, Boolean unique) {
        log.debug("Запрос статистики: период с {} по {}, URIs: {}, уникальные: {}",
                start, end, uris, unique);

        List<ViewStats> stats;
        if (Boolean.TRUE.equals(unique)) {
            stats = statsRepository.getUniqueStats(start, end, uris);
            log.debug("Получена статистика по уникальным посещениям: {} записей", stats.size());
        } else {
            stats = statsRepository.getStats(start, end, uris);
            log.debug("Получена общая статистика: {} записей", stats.size());
        }

        log.debug("Найдено {} записей статистики за указанный период", stats.size());
        return stats;
    }
}