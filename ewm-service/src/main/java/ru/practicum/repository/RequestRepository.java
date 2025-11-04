package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.model.RequestStatus;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с заявками на участие в событиях.
 */
public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {
    /**
     * Находит все заявки пользователя на участие в событиях.
     */
    List<ParticipationRequest> findByRequesterId(Long userId);

    /**
     * Находит все заявки на участие в конкретном событии.
     */
    List<ParticipationRequest> findByEventId(Long eventId);

    /**
     * Находит конкретную заявку пользователя на участие в событии.
     */
    Optional<ParticipationRequest> findByEventIdAndRequesterId(Long eventId, Long userId);

    /**
     * Подсчитывает количество подтвержденных заявок на участие в событии.
     */
    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    /**
     * Находит заявки по списку их ID.
     */
    List<ParticipationRequest> findByIdIn(List<Long> requestIds);

    /**
     * Проверяет, существует ли заявка пользователя на участие в событии.
     */
    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

}
