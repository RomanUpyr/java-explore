package ru.practicum.model;

/**
 * Состояния события
 */
public enum EventState {
    PENDING,    // Ожидание публикации
    PUBLISHED,  // Опубликовано
    CANCELED    // Отменено
}
