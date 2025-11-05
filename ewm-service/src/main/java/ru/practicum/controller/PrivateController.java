package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.*;
import ru.practicum.service.EventService;
import ru.practicum.service.RequestService;

import jakarta.validation.Valid;

import java.util.List;

/**
 * Приватный API для авторизованных пользователей
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class PrivateController {
    private final EventService eventService;
    private final RequestService requestService;

    /**
     * Получение событий, созданных конкретным пользователем
     *
     * @param userId ID пользователя-организатора
     * @param from   начальная позиция
     * @param size   количество элементов на странице
     * @return список событий пользователя в кратком формате
     */
    @GetMapping("/{userId}/events")
    public List<EventShortDto> getEventsByUser(@PathVariable Long userId,
                                               @RequestParam(defaultValue = "0") int from,
                                               @RequestParam(defaultValue = "10") int size) {
        return eventService.getEventsByUser(userId, from, size);
    }

    /**
     * Создание нового события пользователем
     *
     * @param userId      ID пользователя-организатора
     * @param newEventDto данные нового события
     * @return созданное событие с полной информацией
     */
    @PostMapping("/{userId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable Long userId,
                                    @Valid @RequestBody NewEventDto newEventDto) {
        log.debug("CONTROLLER: Received NewEventDto - annotation: '{}', title: '{}'",
                newEventDto.getAnnotation(), newEventDto.getTitle());

        return eventService.createEvent(userId, newEventDto);
    }

    /**
     * Получение конкретного события пользователя
     *
     * @param userId  ID пользователя-организатора
     * @param eventId ID события
     * @return событие с полной информацией
     */
    @GetMapping("/{userId}/events/{eventId}")
    public EventFullDto getEventByUser(@PathVariable Long userId,
                                       @PathVariable Long eventId) {
        return eventService.getEventByUser(userId, eventId);
    }

    /**
     * Обновление события пользователем
     *
     * @param userId        ID пользователя-организатора
     * @param eventId       ID события для обновления
     * @param updateRequest запрос на обновление с новыми данными
     * @return обновленное событие с полной информацией
     */
    @PatchMapping("/{userId}/events/{eventId}")
    public EventFullDto updateEventByUser(@PathVariable Long userId,
                                          @PathVariable Long eventId,
                                          @Valid @RequestBody UpdateEventUserRequest updateRequest) {
        return eventService.updateEventByUser(userId, eventId, updateRequest);
    }

    /**
     * Получение заявок на участие в событиях для пользователя
     *
     * @param userId ID пользователя
     * @return список заявок пользователя
     */
    @GetMapping("/{userId}/requests")
    public List<ParticipationRequestDto> getRequestsByUser(@PathVariable Long userId) {
        return requestService.getRequestsByUser(userId);
    }

    /**
     * Создание новой заявки на участие в событии
     *
     * @param userId  ID пользователя, подающего заявку
     * @param eventId ID события, на которое подается заявка
     * @return созданная заявка
     */
    @PostMapping("/{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable Long userId,
                                                 @RequestParam Long eventId) {
        return requestService.createRequest(userId, eventId);
    }

    /**
     * Отмена заявки на участие пользователем
     *
     * @param userId    ID пользователя
     * @param requestId ID заявки для отмены
     * @return отмененная заявка
     */
    @PatchMapping("/{userId}/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId,
                                                 @PathVariable Long requestId) {
        return requestService.cancelRequest(userId, requestId);
    }

    /**
     * Получение заявок на участие в конкретном событии пользователя
     *
     * @param userId  ID пользователя-организатора
     * @param eventId ID события
     * @return список заявок на участие в событии
     */
    @GetMapping("/{userId}/events/{eventId}/requests")
    public List<ParticipationRequestDto> getRequestsForEvent(@PathVariable Long userId,
                                                             @PathVariable Long eventId) {
        return requestService.getRequestsForEvent(userId, eventId);
    }

    /**
     * Обновление статуса заявок на участие в событии
     *
     * @param userId        ID пользователя-организатора
     * @param eventId       ID события
     * @param updateRequest запрос на изменение статусов заявок
     * @return результат обновления статусов
     */
    @PatchMapping("/{userId}/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestStatus(@PathVariable Long userId,
                                                              @PathVariable Long eventId,
                                                              @RequestBody EventRequestStatusUpdateRequest updateRequest) {
        return requestService.updateRequestStatus(userId, eventId, updateRequest);
    }
}
