package ru.practicum.dto;

import lombok.*;

import java.util.List;

/**
 * DTO для информации об ошибке.
 * Используется для стандартизированного возврата информации об ошибках клиенту.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private List<String> errors;     // Список описаний ошибок
    private String message;          // Сообщение об ошибке
    private String reason;           // Причина ошибки
    private String status;           // HTTP статус
    private String timestamp;        // Время возникновения ошибки
}
