package ru.practicum.dto;

import lombok.*;
import ru.practicum.model.RequestStatus;

/**
 * DTO для заявки на участие
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto {
    private Long id;
    private String created;
    private Long event;
    private Long requester;
    private RequestStatus status;
}
