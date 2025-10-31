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
    private Long id;
    private List<EventShortDto> events;
    private Boolean pinned;
    private String title;
}
