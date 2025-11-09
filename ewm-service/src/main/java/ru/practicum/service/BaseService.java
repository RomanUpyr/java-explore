package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.model.*;
import ru.practicum.repository.*;
import ru.practicum.exception.*;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Базовый сервис с общими методами
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BaseService {
    protected final UserRepository userRepository;
    protected final CategoryRepository categoryRepository;
    protected final EventRepository eventRepository;
    protected final RequestRepository requestRepository;
    protected final CompilationRepository compilationRepository;
    protected final StatsTrackingService statsTrackingService;

    /**
     * Проверяет существование пользователя
     */
    protected User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    /**
     * Проверяет существование категории
     */
    protected Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + categoryId + " was not found"));
    }

    /**
     * Проверяет существование события
     */
    protected Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    /**
     * Получает событие по ID и инициатору
     */
    public Event getEventByIdAndInitiatorId(Long eventId, Long userId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    /**
     * Создает объект пагинации
     */
    protected Pageable createPageRequest(int from, int size) {
        return PageRequest.of(from / size, size);
    }

    /**
     * Парсит строку в LocalDateTime
     */
    protected LocalDateTime parseDateTime(String dateTimeStr) {
        return statsTrackingService.parseDateTime(dateTimeStr);
    }

    /**
     * Форматирует LocalDateTime в строку
     */
    protected String formatDateTime(LocalDateTime dateTime) {
        return statsTrackingService.formatDateTime(dateTime);
    }


    /**
     * Отправляет статистику
     */
    public void sendStats(String clientIp, String uri) {
        statsTrackingService.trackHit(uri, clientIp);
    }

    /**
     * Отправляет статистику на основе HttpServletRequest
     */
    public void sendStats(HttpServletRequest request) {
        statsTrackingService.trackHit(request);
    }

    /**
     * Получает IP клиента
     */
    public String getClientIp(HttpServletRequest request) {
        return statsTrackingService.getClientIp(request);
    }

    /**
     * Проверяет существование пользователя по email
     */
    public boolean userExistsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Проверяет существование категории по имени
     */
    public boolean categoryExistsByName(String name) {
        return categoryRepository.existsByName(name);
    }

    /**
     * Проверяет существование заявки на участие
     */
    public boolean requestExistsByEventIdAndRequesterId(Long eventId, Long requesterId) {
        return requestRepository.existsByEventIdAndRequesterId(eventId, requesterId);
    }

    /**
     * Получает события по категории
     */
    public List<Event> getEventsByCategoryId(Long categoryId) {
        return eventRepository.findByCategoryId(categoryId);
    }

    /**
     * Проверяет существование пользователя
     */
    public boolean userExists(Long userId) {
        return userRepository.existsById(userId);
    }

}
