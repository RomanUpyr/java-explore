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
    private String annotation;                // Новая аннотация

    private Long category;                    // Новая категория

    @Size(min = 20, max = 7000)
    private String description;               // Новое описание

    private String eventDate;                 // Новая дата события
    private Location location;                // Новое местоположение
    private Boolean paid;                     // Новый флаг платности
    private Integer participantLimit;         // Новый лимит участников
    private Boolean requestModeration;        // Новый флаг модерации
    private AdminStateAction stateAction;     //Действие администратора над событием(PUBLISH_EVENT - опубликовать, REJECT_EVENT - отклонить)

    @Size(min = 3, max = 120)
    private String title;                     // Новый заголовок
}
