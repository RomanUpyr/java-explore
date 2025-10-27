package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.EndpointHit;
import ru.practicum.ViewStats;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий для работы с сущностью EndpointHit.
 */
@Repository
public interface StatsRepository extends JpaRepository<EndpointHit, Long> {

    /**
     * Получает общую статистику по посещениям (все).
     */
    @Query("SELECT new ru.practicum.stats.dto.ViewStats(h.app, h.uri, COUNT(h.id)) " +
            "FROM EndpointHit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR h.uri IN :uris) " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h.id) DESC")
    List<ViewStats> getStats(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("uris") List<String> uris);

    /**
     * Получает статистику по уникальным посещениям (уникальные IP).
     */
    @Query("SELECT new ru.practicum.stats.dto.ViewStats(h.app, h.uri, COUNT(DISTINCT h.ip)) " +
            "FROM EndpointHit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR h.uri IN :uris) " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<ViewStats> getUniqueStats(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("uris") List<String> uris);
}
