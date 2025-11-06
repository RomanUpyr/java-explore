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

        baseService.getUserById(userId);

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
            throw new ConflictException("Initiator cannot participate in own event");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in unpublished event");
        }

        if (event.getParticipantLimit() > 0 &&
                event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("The event has reached participant limit");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Request already exists");
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
        log.debug("Request created with id={}", savedRequest.getId());

        return convertToDto(savedRequest);
    }

    /**
     * Отмена заявки пользователем
     */
    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.debug("Canceling request id={} for user id={}", requestId, userId);

        baseService.getUserById(userId);
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));

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
    @Override
    public List<ParticipationRequestDto> getRequestsForEvent(Long userId, Long eventId) {
        log.debug("Getting requests for event id={} by user id={}", eventId, userId);

        baseService.getUserById(userId);
        Event event = baseService.eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

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

        baseService.getUserById(userId);
        Event event = baseService.eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

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
