package ru.practicum.dto;

import jakarta.validation.constraints.Size;
import lombok.*;
import ru.practicum.model.AdminStateAction;
import ru.practicum.model.Location;

/**
 * DTO для обновления события администратором
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventAdminRequest {
    @Size(min = 20, max = 2000)
    private String annotation;

    private Long category;

    @Size(min = 20, max = 7000)
    private String description;

    private String eventDate;
    private Location location;
    private Boolean paid;
    private Integer participantLimit;
    private Boolean requestModeration;
    private AdminStateAction stateAction;

    @Size(min = 3, max = 120)
    private String title;
}
