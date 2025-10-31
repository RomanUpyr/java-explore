package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.*;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.*;
import ru.practicum.repository.RequestRepository;
import ru.practicum.exception.ConflictException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с заявками на участие в событиях
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService extends BaseService {
    /**
     * Получение заявок пользователя
     */
    public List<ParticipationRequestDto> getRequestsByUser(Long userId) {
        log.info("Getting requests for user id={}", userId);

        getUserById(userId);

        return requestRepository.findByRequesterId(userId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Создание заявки на участие
     */
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Creating request for user id={} to event id={}", userId, eventId);

        User user = getUserById(userId);
        Event event = getEventById(eventId);

        // Нельзя участвовать в своем событии
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot participate in own event");
        }

        // Событие должно быть опубликовано
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in unpublished event");
        }

        // Проверяем лимит участников
        if (event.getParticipantLimit() > 0 &&
                event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("The event has reached participant limit");
        }

        // Оптимизированная проверка дублирования заявки
        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Request already exists");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(user)
                .status(RequestStatus.PENDING)
                .build();

        // Если премодерация отключена, подтверждаем автоматически
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        }

        ParticipationRequest savedRequest = requestRepository.save(request);
        log.info("Request created with id={}", savedRequest.getId());

        return convertToDto(savedRequest);
    }

    /**
     * Отмена заявки пользователем
     */
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Canceling request id={} for user id={}", requestId, userId);

        getUserById(userId);
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));

        // Проверяем, что заявка принадлежит пользователю
        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Request with id=" + requestId + " was not found");
        }

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);

        return convertToDto(updatedRequest);
    }

    /**
     * Получение заявок на участие в событии пользователя
     */
    public List<ParticipationRequestDto> getRequestsForEvent(Long userId, Long eventId) {
        log.info("Getting requests for event id={} by user id={}", eventId, userId);

        getUserById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        return requestRepository.findByEventId(eventId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Изменение статуса заявок на участие
     */
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        log.info("Updating request status for event id={} by user id={}: {}", eventId, userId, updateRequest);

        getUserById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        // Проверяем лимит участников
        if (event.getParticipantLimit() > 0 &&
                event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("The event has reached participant limit");
        }

        List<ParticipationRequest> requests = requestRepository.findByIdIn(updateRequest.getRequestIds());

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }

            if (updateRequest.getStatus() == RequestStatus.CONFIRMED) {
                if (event.getConfirmedRequests() < event.getParticipantLimit()) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                    result.getConfirmedRequests().add(convertToDto(request));
                } else {
                    request.setStatus(RequestStatus.REJECTED);
                    result.getRejectedRequests().add(convertToDto(request));
                }
            } else {
                request.setStatus(RequestStatus.REJECTED);
                result.getRejectedRequests().add(convertToDto(request));
            }
        }

        eventRepository.save(event);
        requestRepository.saveAll(requests);

        return result;
    }

    private ParticipationRequestDto convertToDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(formatDateTime(request.getCreated()))
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus())
                .build();
    }
}
