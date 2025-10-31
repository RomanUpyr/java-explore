package ru.practicum.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import ru.practicum.model.Location;

/**
 * DTO для создания нового события
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {
    @NotBlank(message = "Annotation cannot be blank")
    @Size(min = 20, max = 2000, message = "Annotation must be between 20 and 2000 characters")
    private String annotation;

    @NotNull(message = "Category cannot be null")
    private Long category;

    @NotBlank(message = "Description cannot be blank")
    @Size(min = 20, max = 7000, message = "Description must be between 20 and 7000 characters")
    private String description;

    @NotBlank(message = "Event date cannot be blank")
    private String eventDate;

    @NotNull(message = "Location cannot be null")
    private Location location;

    private Boolean paid = false;
    private Integer participantLimit = 0;
    private Boolean requestModeration = true;

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 3, max = 120, message = "Title must be between 3 and 120 characters")
    private String title;
}
