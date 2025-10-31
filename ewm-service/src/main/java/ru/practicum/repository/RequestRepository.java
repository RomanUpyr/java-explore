package ru.practicum.repository;

import ru.practicum.model.ParticipationRequest;
import ru.practicum.model.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface RequestRepository {
    List<ParticipationRequest> findByRequesterId(Long userId);
    List<ParticipationRequest> findByEventId(Long eventId);
    Optional<ParticipationRequest> findByEventIdAndRequesterId(Long eventId, Long userId);
    Long countByEventIdAndStatus(Long eventId, RequestStatus status);
    List<ParticipationRequest> findByIdIn(List<Long> requestIds);
    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);
}
