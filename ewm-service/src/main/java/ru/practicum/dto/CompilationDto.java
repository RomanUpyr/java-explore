package ru.practicum.dto;

import lombok.*;

import java.util.List;

/**
 * DTO для подборки событий
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationDto {
    private Long id;                         // Уникальный идентификатор подборки
    private List<EventShortDto> events;      // Список событий в подборке в кратком формате
    private Boolean pinned;                  // Флаг закрепления подборки (отображается ли на главной странице)
    private String title;                    // Заголовок подборки
}
