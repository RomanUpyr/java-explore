package ru.practicum.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import ru.practicum.model.Location;
import ru.practicum.model.UserStateAction;

/**
 * DTO для обновления события пользователем
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventUserRequest {
    @Size(min = 20, max = 2000)
    private String annotation;                // Новая аннотация

    private Long category;                    // Новая категория

    @Size(min = 20, max = 7000)
    private String description;               // Новое описание

    private String eventDate;                 // Новая дата события
    private Location location;                // Новое местоположение
    private Boolean paid;                     // Новый флаг платности
    @Min(0)
    private Integer participantLimit;         // Новый лимит участников
    private Boolean requestModeration;        // Новый флаг модерации
    private UserStateAction stateAction;      // Действие пользователя над событием (SEND_TO_REVIEW - отправить на модерацию, CANCEL_REVIEW - отменить отправку)

    @Size(min = 3, max = 120)
    private String title;                     // Новый заголовок
}
