package ru.practicum.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO для результата изменения статуса заявок на участие
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateResult {
    @Builder.Default
    private List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();       // Список подтвержденных заявок
    @Builder.Default
    private List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();       // Список отклоненных заявок

}
