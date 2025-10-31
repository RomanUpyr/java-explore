package ru.practicum.dto;

import lombok.*;
import ru.practicum.model.RequestStatus;
import java.util.List;

/**
 * DTO для изменения статуса заявок
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {
    private List<Long> requestIds;
    private RequestStatus status;
}
