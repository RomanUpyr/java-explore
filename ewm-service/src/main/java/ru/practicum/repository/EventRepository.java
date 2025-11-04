package ru.practicum.repository;


import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с событиями.
 */
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    /**
     * Находит события конкретного пользователя.
     */
    List<Event> findByInitiatorId(Long userId, Pageable pageable);

    /**
     * Находит конкретное событие по ID и ID инициатора.
     */
    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    /**
     * Находит события по списку ID.
     */
    List<Event> findByIdIn(List<Long> events);

    /**
     * Находит все события определенной категории.
     */
    List<Event> findByCategoryId(Long categoryId);

    /**
     * Поиск событий по комплексным фильтрам для администратора.
     * Все параметры являются опциональными (nullable).
     */
    @Query("SELECT e FROM Event e WHERE " +
            "(:users IS NULL OR e.initiator.id IN :users) AND " +
            "(:states IS NULL OR e.state IN :states) AND " +
            "(:categories IS NULL OR e.category.id IN :categories) AND " +
            "(:rangeStart IS NULL OR e.eventDate >= :rangeStart) AND " +
            "(:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)")
    List<Event> findEventsByFilters(@Param("users") List<Long> users,
                                    @Param("states") List<EventState> states,
                                    @Param("categories") List<Long> categories,
                                    @Param("rangeStart") LocalDateTime rangeStart,
                                    @Param("rangeEnd") LocalDateTime rangeEnd,
                                    Pageable pageable);
}
