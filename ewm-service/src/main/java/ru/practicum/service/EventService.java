package ru.practicum.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.dto.*;

import java.util.List;

/**
 * Сервис для работы с событиями
 */
public interface EventService {
    List<EventShortDto> getEventsByUser(Long userId, int from, int size);

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    EventFullDto getEventByUser(Long userId, Long eventId);

    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest);

    List<EventFullDto> getEventsForAdmin(List<Long> users, List<String> states, List<Long> categories,
                                         String rangeStart, String rangeEnd, int from, int size);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest);

    List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                        String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                        String sort, int from, int size, HttpServletRequest request);

    EventFullDto getEventPublic(Long eventId, HttpServletRequest request);

}
