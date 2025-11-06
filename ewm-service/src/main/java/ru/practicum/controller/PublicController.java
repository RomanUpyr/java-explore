package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.EndpointHitRequest;
import ru.practicum.StatsClient;
import ru.practicum.dto.*;
import ru.practicum.service.CategoryService;
import ru.practicum.service.CompilationService;
import ru.practicum.service.EventService;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Публичный API
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class PublicController {
    private final EventService eventService;
    private final CategoryService categoryService;
    private final CompilationService compilationService;
    private final StatsClient statsClient;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Поиск и фильтрация событий для публичного доступа
     *
     * @param text          текст для поиска в аннотации и описании (опционально)
     * @param categories    список ID категорий для фильтрации (опционально)
     * @param paid          фильтр по платным/бесплатным событиям (опционально)
     * @param rangeStart    начальная дата диапазона (опционально)
     * @param rangeEnd      конечная дата диапазона (опционально)
     * @param onlyAvailable только события с доступными местами (по умолчанию false)
     * @param sort          тип сортировки (опционально)
     * @param from          начальная позиция
     * @param size          количество элементов на странице
     * @param request       HTTP запрос для получения IP клиента
     * @return список событий в кратком формате
     */
    @GetMapping("/events")
    public List<EventShortDto> getEventsPublic(@RequestParam(required = false) String text,
                                               @RequestParam(required = false) List<Long> categories,
                                               @RequestParam(required = false) Boolean paid,
                                               @RequestParam(required = false) String rangeStart,
                                               @RequestParam(required = false) String rangeEnd,
                                               @RequestParam(defaultValue = "false") Boolean onlyAvailable,
                                               @RequestParam(required = false) String sort,
                                               @RequestParam(defaultValue = "0") int from,
                                               @RequestParam(defaultValue = "10") int size,
                                               HttpServletRequest request) {
        String clientIp = getClientIp(request);

        saveHit("/events", clientIp, request);

        return eventService.getEventsPublic(text, categories, paid, rangeStart, rangeEnd,
                onlyAvailable, sort, from, size, clientIp);
    }

    /**
     * Получение полной информации о конкретном событии
     *
     * @param id      ID события
     * @param request HTTP запрос для получения IP клиента
     * @return событие с полной информацией
     */
    @GetMapping("/events/{id}")
    public EventFullDto getEventPublic(@PathVariable Long id,
                                       HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String uri = "/events/" + id;
        saveHit(uri, clientIp, request);
        return eventService.getEventPublic(id, clientIp);
    }

    /**
     * Получение списка категорий
     *
     * @param from начальная позиция
     * @param size количество элементов на странице
     * @return список категорий
     */
    @GetMapping("/categories")
    public List<CategoryDto> getCategories(@RequestParam(defaultValue = "0") int from,
                                           @RequestParam(defaultValue = "10") int size,
                                           HttpServletRequest request) {
        saveHit("/categories", getClientIp(request), request);

        return categoryService.getCategories(from, size);
    }

    /**
     * Получение информации о конкретной категории
     *
     * @param categoryId ID категории
     * @return информация о категории
     */
    @GetMapping("/categories/{categoryId}")
    public CategoryDto getCategory(@PathVariable Long categoryId,
                                   HttpServletRequest request) {
        String uri = "/categories/" + categoryId;
        saveHit(uri, getClientIp(request), request);

        return categoryService.getCategory(categoryId);
    }

    /**
     * Получение подборок событий
     *
     * @param pinned фильтр по закрепленным подборкам (опционально)
     * @param from   начальная позиция
     * @param size   количество элементов на странице
     * @return список подборок
     */
    @GetMapping("/compilations")
    public List<CompilationDto> getCompilations(@RequestParam(required = false) Boolean pinned,
                                                @RequestParam(defaultValue = "0") int from,
                                                @RequestParam(defaultValue = "10") int size,
                                                HttpServletRequest request) {
        saveHit("/compilations", getClientIp(request), request);

        return compilationService.getCompilations(pinned, from, size);
    }

    /**
     * Получение информации о конкретной подборке
     *
     * @param compilationId ID подборки
     * @return информация о подборке
     */
    @GetMapping("/compilations/{compilationId}")
    public CompilationDto getCompilation(@PathVariable Long compilationId,
                                         HttpServletRequest request) {
        String uri = "/compilations/" + compilationId;
        saveHit(uri, getClientIp(request), request);

        return compilationService.getCompilation(compilationId);
    }

    /**
     * Сохраняет информацию о посещении эндпоинта через StatsClient
     */
    private void saveHit(String uri, String clientIp, HttpServletRequest request) {
        try {
            EndpointHitRequest hitRequest = EndpointHitRequest.builder()
                    .app("ewm-main-service")
                    .uri(uri)
                    .ip(clientIp)
                    .timestamp(LocalDateTime.now().format(FORMATTER))
                    .build();

            statsClient.saveHit(hitRequest);

        } catch (Exception e) {
            System.err.println("Ошибка при сохранении статистики для URI: " + uri + " - " + e.getMessage());
        }
    }

    /**
     * Вспомогательный метод для получения реального IP адреса клиента
     *
     * @param request HTTP запрос
     * @return IP адрес клиента
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
