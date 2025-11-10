package ru.practicum.dto;

import lombok.*;
import ru.practicum.model.RequestStatus;

/**
 * DTO для заявки на участие в событии
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto {
    private Long id;                   // Уникальный идентификатор заявки
    private String created;            // Дата и время создания заявки
    private Long event;                // Идентификатор события
    private Long requester;            // Идентификатор пользователя-заявителя
    private RequestStatus status;      // Статус заявки (PENDING, CONFIRMED, REJECTED, CANCELED)
}
