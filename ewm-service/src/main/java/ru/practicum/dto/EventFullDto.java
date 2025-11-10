package ru.practicum.dto;

import lombok.*;
import ru.practicum.model.EventState;
import ru.practicum.model.Location;

/**
 * DTO для полной информации о событии
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventFullDto {
    private Long id;                          // Уникальный идентификатор события
    private String annotation;                // Краткое описание события
    private CategoryDto category;             // Категория события в формате DTO
    private Integer confirmedRequests;        // Количество подтвержденных запросов на участие
    private String createdOn;                 // Дата и время создания события
    private String description;               // Полное описание события
    private String eventDate;                 // Дата и время проведения события
    private UserShortDto initiator;           // Инициатор события в кратком формате
    private Location location;                // Местоположение события
    private Boolean paid;                     // Флаг платности события
    private Integer participantLimit;         // Ограничение количества участников
    private String publishedOn;               // Дата и время публикации события
    private Boolean requestModeration;        // Требуется ли модерация заявок на участие
    private EventState state;                 // Текущее состояние события (PENDING, PUBLISHED, CANCELED)
    private String title;                     // Заголовок события
    private Long views;                       // Количество просмотров события
    private Long commentsCount;               // Количество комментариев
}
