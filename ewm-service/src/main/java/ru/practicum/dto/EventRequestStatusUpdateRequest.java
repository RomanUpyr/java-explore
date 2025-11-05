package ru.practicum.dto;

import lombok.*;
import ru.practicum.model.RequestStatus;

import java.util.List;

/**
 * DTO для запроса на изменение статуса заявок на участие в событии
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {
    private List<Long> requestIds;         // Список идентификаторов заявок для изменения статуса
    private RequestStatus status;          // Новый статус (CONFIRMED - подтверждено, REJECTED - отклонено)
}
