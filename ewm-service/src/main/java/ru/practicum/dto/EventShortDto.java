package ru.practicum.dto;

import lombok.*;

/**
 * DTO для краткой информации о событии
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventShortDto {
    private Long id;                       // Уникальный идентификатор события
    private String annotation;             // Краткое описание события
    private CategoryDto category;          // Категория события
    private Integer confirmedRequests;     // Количество подтвержденных заявок
    private String eventDate;              // Дата и время проведения события
    private UserShortDto initiator;        // Организатор события
    private Boolean paid;                  // Флаг платности события
    private String title;                  // Заголовок события
    private Long views;                    // Количество просмотров
}
