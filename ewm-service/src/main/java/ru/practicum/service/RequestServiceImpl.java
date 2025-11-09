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
 * Реализация сервиса для работы с заявками на участие в событиях
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final BaseService baseService;

    /**
     * Получение заявок пользователя
     */
    @Override
    public List<ParticipationRequestDto> getRequestsByUser(Long userId) {
        log.debug("Getting requests for user id={}", userId);

        if (!baseService.userExists(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        return requestRepository.findByRequesterId(userId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Создание заявки на участие
     */
    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.debug("Creating request for user id={} to event id={}", userId, eventId);

        User user = baseService.getUserById(userId);
        Event event = baseService.getEventById(eventId);

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException(String.format("User id=%d cannot participate in own event id=%d.",
                    userId, eventId));
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException(String.format("Cannot participate in unpublished event id=%d. Current state: %s.",
                    eventId, event.getState()));
        }

        if (event.getParticipantLimit() > 0 &&
                event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException(String.format("Event id=%d has reached participant limit. Limit: %d, Current: %d.",
                    eventId, event.getParticipantLimit(), event.getConfirmedRequests()));
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException(String.format("Request already exists for user id=%d to event id=%d.",
                    userId, eventId));
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(user)
                .status(RequestStatus.PENDING)
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            baseService.eventRepository.save(event);
        }

        ParticipationRequest savedRequest = requestRepository.save(request);
        log.debug("Request created with id={} for user id={} to event id={}",
                savedRequest.getId(), userId, eventId);

        return convertToDto(savedRequest);
    }

    /**
     * Отмена заявки пользователем
     */
    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.debug("Canceling request id={} for user id={}", requestId, userId);

        if (!baseService.userExists(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(String.format("Request id=%d not found for user id=%d.",
                        requestId, userId)));

        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException(String.format("Request id=%d does not belong to user id=%d. Actual owner: user id=%d.",
                    requestId, userId, request.getRequester().getId()));
        }

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);

        log.debug("Request id={} canceled by user id={}", requestId, userId);

        return convertToDto(updatedRequest);
    }

    /**
     * Получение заявок на участие в событии пользователя
     */
    @Override
    public List<ParticipationRequestDto> getRequestsForEvent(Long userId, Long eventId) {
        log.debug("Getting requests for event id={} by user id={}", eventId, userId);

        if (!baseService.userExists(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        Event event = baseService.eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(String.format("Event id=%d not found or user id=%d is not initiator.",
                        eventId, userId)));

        return requestRepository.findByEventId(eventId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Изменение статуса заявок на участие
     */
    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        log.debug("Updating request status for event id={} by user id={}: {}", eventId, userId, updateRequest);

        if (!baseService.userExists(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        Event event = baseService.eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(String.format("Event id=%d not found or user id=%d is not initiator.",
                        eventId, userId)
                ));

        if (event.getParticipantLimit() > 0 &&
                event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException(String.format("Event id=%d has reached participant limit. Limit: %d, Current: %d. User id=%d. [REQUEST:EVENT_FULL]",
                    eventId, event.getParticipantLimit(), event.getConfirmedRequests(), userId));
        }

        List<ParticipationRequest> requests = requestRepository.findByIdIn(updateRequest.getRequestIds());

        if (requests.size() != updateRequest.getRequestIds().size()) {
            log.warn("Some requests not found. Requested: {}, Found: {}. User id={}, Event id={}",
                    updateRequest.getRequestIds().size(), requests.size(), userId, eventId);
        }

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException(String.format("Request id=%d must have status PENDING. Current status: %s. User id=%d, Event id=%d.",
                        request.getId(), request.getStatus(), userId, eventId));
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

        baseService.eventRepository.save(event);
        requestRepository.saveAll(requests);

        return result;
    }

    private ParticipationRequestDto convertToDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(baseService.formatDateTime(request.getCreated()))
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus())
                .build();
    }
}
