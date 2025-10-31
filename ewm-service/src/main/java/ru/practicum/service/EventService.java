package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.*;
import ru.practicum.model.*;
import ru.practicum.repository.EventRepository;
import ru.practicum.exception.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для работы с событиями
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventService extends BaseService {

    /**
     * Получение событий, добавленных текущим пользователем
     */
    public List<EventShortDto> getEventsByUser(Long userId, int from, int size) {
        log.info("Getting events for user id={}, from={}, size={}", userId, from, size);

        getUserById(userId); // Проверяем существование пользователя

        return eventRepository.findByInitiatorId(userId, createPageRequest(from, size))
                .stream()
                .map(this::convertToShortDto)
                .collect(Collectors.toList());
    }

    /**
     * Создание нового события
     */
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("Creating event for user id={}: {}", userId, newEventDto);

        User user = getUserById(userId);
        Category category = getCategoryById(newEventDto.getCategory());

        // Проверяем, что дата события не раньше чем через 2 часа от текущего момента
        LocalDateTime eventDate = parseDateTime(newEventDto.getEventDate());
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours from now");
        }

        Event event = Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(category)
                .description(newEventDto.getDescription())
                .eventDate(eventDate)
                .initiator(user)
                .location(new ru.practicum.model.Location(
                        newEventDto.getLocation().getLat(),
                        newEventDto.getLocation().getLon()))
                .paid(newEventDto.getPaid() != null ? newEventDto.getPaid() : false)
                .participantLimit(newEventDto.getParticipantLimit() != null ? newEventDto.getParticipantLimit() : 0)
                .requestModeration(newEventDto.getRequestModeration() != null ? newEventDto.getRequestModeration() : true)
                .title(newEventDto.getTitle())
                .createdOn(LocalDateTime.now())
                .state(EventState.PENDING)
                .confirmedRequests(0)
                .build();

        Event savedEvent = eventRepository.save(event);
        log.info("Event created with id={}", savedEvent.getId());

        return convertToFullDto(savedEvent);
    }

    /**
     * Получение полной информации о событии пользователя
     */
    public EventFullDto getEventByUser(Long userId, Long eventId) {
        log.info("Getting event id={} for user id={}", eventId, userId);

        getUserById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        return convertToFullDto(event);
    }

    /**
     * Обновление события пользователем
     */
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("Updating event id={} for user id={}: {}", eventId, userId, updateRequest);

        getUserById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        // Можно обновлять только события в состоянии PENDING или CANCELED
        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        updateEventFields(event, updateRequest);

        // Обработка изменения состояния
        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        return convertToFullDto(updatedEvent);
    }

    /**
     * Получение событий с фильтрацией для администратора
     */
    public List<EventFullDto> getEventsForAdmin(List<Long> users, List<String> states,
                                                List<Long> categories, String rangeStart,
                                                String rangeEnd, int from, int size) {
        log.info("Getting events for admin: users={}, states={}, categories={}", users, states, categories);

        // Здесь должна быть реализация с использованием Specification
        // Для простоты возвращаем все события с пагинацией
        return eventRepository.findAll(createPageRequest(from, size))
                .stream()
                .map(this::convertToFullDto)
                .collect(Collectors.toList());
    }

    /**
     * Обновление события администратором
     */
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("Updating event id={} by admin: {}", eventId, updateRequest);

        Event event = getEventById(eventId);

        updateEventFields(event, updateRequest);

        // Обработка изменения состояния администратором
        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case PUBLISH_EVENT:
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
                    }
                    if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                        throw new ConflictException("Cannot publish the event because event date is too soon");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Cannot reject the event because it's already published");
                    }
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        return convertToFullDto(updatedEvent);
    }

    /**
     * Публичный поиск событий
     */
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                               String sort, int from, int size, String clientIp) {
        log.info("Public events search: text={}, categories={}", text, categories);

        // Отправляем статистику о просмотре
        sendStats(clientIp, "/events");

        // Здесь должна быть сложная логика фильтрации
        // Для простоты возвращаем опубликованные события
        List<Event> events = eventRepository.findAll()
                .stream()
                .filter(event -> event.getState() == EventState.PUBLISHED)
                .collect(Collectors.toList());

        return events.stream()
                .map(this::convertToShortDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение события по ID для публичного доступа
     */
    public EventFullDto getEventPublic(Long eventId, String clientIp) {
        log.info("Getting public event id={}", eventId);

        Event event = getEventById(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        // Отправляем статистику о просмотре
        sendStats(clientIp, "/events/" + eventId);

        return convertToFullDto(event);
    }

    /**
     * Обновление полей события
     */
    private void updateEventFields(Event event, UpdateEventUserRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getCategory() != null) {
            Category category = getCategoryById(updateRequest.getCategory());
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            LocalDateTime eventDate = parseDateTime(updateRequest.getEventDate());
            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BadRequestException("Event date must be at least 2 hours from now");
            }
            event.setEventDate(eventDate);
        }
        if (updateRequest.getLocation() != null) {
            event.setLocation(new ru.practicum.model.Location(
                    updateRequest.getLocation().getLat(),
                    updateRequest.getLocation().getLon()));
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
    }


    /**
     * Конвертация Event в EventFullDto
     */
    private EventFullDto convertToFullDto(Event event) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(convertToCategoryDto(event.getCategory()))
                .confirmedRequests(event.getConfirmedRequests())
                .createdOn(formatDateTime(event.getCreatedOn()))
                .description(event.getDescription())
                .eventDate(formatDateTime(event.getEventDate()))
                .initiator(convertToUserShortDto(event.getInitiator()))
                .location(convertToLocationDto(event.getLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn() != null ? formatDateTime(event.getPublishedOn()) : null)
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .views(event.getViews())
                .build();
    }

    /**
     * Конвертация Event в EventShortDto
     */
    private EventShortDto convertToShortDto(Event event) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(convertToCategoryDto(event.getCategory()))
                .confirmedRequests(event.getConfirmedRequests())
                .eventDate(formatDateTime(event.getEventDate()))
                .initiator(convertToUserShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(event.getViews())
                .build();
    }

    private CategoryDto convertToCategoryDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    private UserShortDto convertToUserShortDto(User user) {
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }

    private Location convertToLocationDto(ru.practicum.model.Location location) {
        return Location.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }
}
