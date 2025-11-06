package ru.practicum.repository;


import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.practicum.model.Event;

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

}
