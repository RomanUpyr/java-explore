package ru.practicum.dto;
import lombok.*;
import java.util.List;

/**
 * Результат изменения статуса заявок
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateResult {
    private List<ParticipationRequestDto> confirmedRequests;
    private List<ParticipationRequestDto> rejectedRequests;

}
