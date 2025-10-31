package ru.practicum.model;

/**
 * Статусы заявок на участие в событии
 */
public enum RequestStatus {
    PENDING,    // В ожидании
    CONFIRMED,  // Подтверждено
    REJECTED,   // Отклонено
    CANCELED    // Отменено пользователем
}
