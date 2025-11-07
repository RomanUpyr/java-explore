package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.*;
import ru.practicum.model.*;
import ru.practicum.repository.EventRepository;
import ru.practicum.exception.*;
import ru.practicum.model.Location;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Реализация сервиса для работы с событиями
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final BaseService baseService;


    /**
     * Получение событий, добавленных текущим пользователем
     */
    @Override
    public List<EventShortDto> getEventsByUser(Long userId, int from, int size) {
        log.debug("Getting events for user id={}, from={}, size={}", userId, from, size);

        baseService.getUserById(userId);

        return eventRepository.findByInitiatorId(userId, baseService.createPageRequest(from, size))
                .stream()
                .map(this::convertToShortDto)
                .collect(Collectors.toList());
    }

    /**
     * Создание нового события
     */
    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.debug("Creating event for user id={}: {}", userId, newEventDto);

        User user = baseService.getUserById(userId);
        Category category = baseService.getCategoryById(newEventDto.getCategory());

        LocalDateTime eventDate = baseService.parseDateTime(newEventDto.getEventDate());
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
        log.debug("Event created with id={}", savedEvent.getId());

        return convertToFullDto(savedEvent);
    }

    /**
     * Получение полной информации о событии пользователя
     */
    @Override
    public EventFullDto getEventByUser(Long userId, Long eventId) {
        log.debug("Getting event id={} for user id={}", eventId, userId);

        baseService.getUserById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        return convertToFullDto(event);
    }

    /**
     * Обновление события пользователем
     */
    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.debug("Updating event id={} for user id={}: {}", eventId, userId, updateRequest);

        baseService.getUserById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        updateEventFieldsUser(event, updateRequest);

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
    @Override
    public List<EventFullDto> getEventsForAdmin(List<Long> users, List<String> states,
                                                List<Long> categories, String rangeStart,
                                                String rangeEnd, int from, int size) {
        log.debug("Getting events for admin: users={}, states={}, categories={}, rangeStart={}, rangeEnd={}",
                users, states, categories, rangeStart, rangeEnd);

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (rangeStart != null) {
            startDateTime = baseService.parseDateTime(rangeStart);
        }
        if (rangeEnd != null) {
            endDateTime = baseService.parseDateTime(rangeEnd);
        }

        if (startDateTime != null && endDateTime != null && endDateTime.isBefore(startDateTime)) {
            throw new BadRequestException("Range end cannot be before range start");
        }
        Specification<Event> spec = Specification.where(null);

        if (users != null && !users.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("initiator").get("id").in(users));
        }

        if (states != null && !states.isEmpty()) {
            List<EventState> eventStates = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
            spec = spec.and((root, query, cb) -> root.get("state").in(eventStates));
        }

        if (categories != null && !categories.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("category").get("id").in(categories));
        }

        if (rangeStart != null) {
            LocalDateTime start = baseService.parseDateTime(rangeStart);
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("eventDate"), start));
        }

        if (rangeEnd != null) {
            LocalDateTime end = baseService.parseDateTime(rangeEnd);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("eventDate"), end));
        }

        List<Event> events = eventRepository.findAll(spec, baseService.createPageRequest(from, size)).getContent();

        log.debug("Found {} events after filtering", events.size());

        return events.stream()
                .map(this::convertToFullDto)
                .collect(Collectors.toList());
    }

    /**
     * Обновление события администратором
     */
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.debug("Updating event id={} by admin: {}", eventId, updateRequest);

        Event event = baseService.getEventById(eventId);
        log.debug("Event BEFORE update - Annotation: '{}', Title: '{}'",
                event.getAnnotation(), event.getTitle());
        updateEventFields(event, updateRequest);

        log.debug("Event AFTER update fields - Annotation: '{}', Title: '{}'",
                event.getAnnotation(), event.getTitle());

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
        log.debug("Event FINAL state - Annotation: '{}', Title: '{}'",
                updatedEvent.getAnnotation(), updatedEvent.getTitle());
        return convertToFullDto(updatedEvent);
    }

    /**
     * Публичный поиск событий
     */
    @Override
    @Transactional
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                               String sort, int from, int size, String clientIp) {
        log.debug("Public events search: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, onlyAvailable={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable);

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (rangeStart != null) {
            startDateTime = baseService.parseDateTime(rangeStart);
        }
        if (rangeEnd != null) {
            endDateTime = baseService.parseDateTime(rangeEnd);
        }

        if (startDateTime != null && endDateTime != null && endDateTime.isBefore(startDateTime)) {
            throw new BadRequestException("Range end cannot be before range start");
        }

        Specification<Event> spec = Specification.where((root, query, cb) ->
                cb.equal(root.get("state"), EventState.PUBLISHED));

        if (text != null && !text.isEmpty() && !text.equals("0")) {
            String searchText = "%" + text.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("annotation")), searchText),
                            cb.like(cb.lower(root.get("description")), searchText)
                    ));
        }

        if (categories != null && !categories.isEmpty() && !categories.contains(0L)) {
            spec = spec.and((root, query, cb) -> root.get("category").get("id").in(categories));
        }

        if (paid != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("paid"), paid));
        }

        final LocalDateTime finalStartDateTime;
        if (rangeStart != null) {
            finalStartDateTime = baseService.parseDateTime(rangeStart);
        } else {
            finalStartDateTime = LocalDateTime.now();
        }
        spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("eventDate"), finalStartDateTime));

        if (rangeEnd != null) {
            final LocalDateTime finalEndDateTime = baseService.parseDateTime(rangeEnd);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("eventDate"), finalEndDateTime));
        }

        List<Event> events = eventRepository.findAll(spec, baseService.createPageRequest(from, size)).getContent();

        events.forEach(event -> {
            event.setViews(event.getViews());

        });
        List<Event> updatedEvents = eventRepository.saveAll(events);
        log.debug("Increased views for {} events", updatedEvents.size());

        if (Boolean.TRUE.equals(onlyAvailable)) {
            events = events.stream()
                    .filter(event -> event.getParticipantLimit() == 0 ||
                            event.getConfirmedRequests() < event.getParticipantLimit())
                    .collect(Collectors.toList());
        }

        List<Event> sortedEvents = sortEvents(events, sort);

        log.debug("Found {} events after public filtering", sortedEvents.size());

        return sortedEvents.stream()
                .map(this::convertToShortDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение события по ID для публичного доступа
     */
    @Override
    @Transactional
    public EventFullDto getEventPublic(Long eventId, String clientIp) {
        log.debug("Getting public event id={}", eventId);

        Event event = baseService.getEventById(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        event.setViews(event.getViews() + 1);
        Event updatedEvent = eventRepository.save(event);
        log.debug("Event id={} views increased to {}", eventId, updatedEvent.getViews());

        return convertToFullDto(event);
    }

    /**
     * Обновление полей события администратором
     */
    private void updateEventFields(Event event, UpdateEventAdminRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getCategory() != null) {
            Category category = baseService.getCategoryById(updateRequest.getCategory());
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            LocalDateTime eventDate = baseService.parseDateTime(updateRequest.getEventDate());
            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BadRequestException("Event date must be at least 2 hours from now");
            }
            event.setEventDate(eventDate);
        }
        if (updateRequest.getLocation() != null) {
            event.setLocation(new Location(
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
     * Обновление полей события пользователем
     */
    private void updateEventFieldsUser(Event event, UpdateEventUserRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getCategory() != null) {
            Category category = baseService.getCategoryById(updateRequest.getCategory());
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            LocalDateTime eventDate = baseService.parseDateTime(updateRequest.getEventDate());
            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BadRequestException("Event date must be at least 2 hours from now");
            }
            event.setEventDate(eventDate);
        }
        if (updateRequest.getLocation() != null) {
            event.setLocation(new Location(
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
                .createdOn(baseService.formatDateTime(event.getCreatedOn()))
                .description(event.getDescription())
                .eventDate(baseService.formatDateTime(event.getEventDate()))
                .initiator(convertToUserShortDto(event.getInitiator()))
                .location(convertToLocationDto(event.getLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn() != null ? baseService.formatDateTime(event.getPublishedOn()) : null)
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
                .eventDate(baseService.formatDateTime(event.getEventDate()))
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

    /**
     * Сортировка событий
     */
    private List<Event> sortEvents(List<Event> events, String sort) {
        if (sort == null) {
            return events;
        }

        switch (sort.toUpperCase()) {
            case "EVENT_DATE":
                return events.stream()
                        .sorted(Comparator.comparing(Event::getEventDate))
                        .collect(Collectors.toList());
            case "VIEWS":
                return events.stream()
                        .sorted(Comparator.comparing(Event::getViews).reversed())
                        .collect(Collectors.toList());
            default:
                return events;
        }
    }
}

