package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.*;
import ru.practicum.service.CategoryService;
import ru.practicum.service.CompilationService;
import ru.practicum.service.EventService;

import jakarta.servlet.http.HttpServletRequest;
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

    // Events
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
        return eventService.getEventsPublic(text, categories, paid, rangeStart, rangeEnd,
                onlyAvailable, sort, from, size, clientIp);
    }

    @GetMapping("/events/{id}")
    public EventFullDto getEventPublic(@PathVariable Long id,
                                       HttpServletRequest request) {
        String clientIp = getClientIp(request);
        return eventService.getEventPublic(id, clientIp);
    }

    // Categories
    @GetMapping("/categories")
    public List<CategoryDto> getCategories(@RequestParam(defaultValue = "0") int from,
                                           @RequestParam(defaultValue = "10") int size) {
        return categoryService.getCategories(from, size);
    }

    @GetMapping("/categories/{categoryId}")
    public CategoryDto getCategory(@PathVariable Long categoryId) {
        return categoryService.getCategory(categoryId);
    }

    // Compilations
    @GetMapping("/compilations")
    public List<CompilationDto> getCompilations(@RequestParam(required = false) Boolean pinned,
                                                @RequestParam(defaultValue = "0") int from,
                                                @RequestParam(defaultValue = "10") int size) {
        return compilationService.getCompilations(pinned, from, size);
    }

    @GetMapping("/compilations/{compilationId}")
    public CompilationDto getCompilation(@PathVariable Long compilationId) {
        return compilationService.getCompilation(compilationId);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
