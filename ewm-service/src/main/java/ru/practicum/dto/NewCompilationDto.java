package ru.practicum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;

/**
 * DTO для создания новой подборки
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {
    private List<Long> events;
    private Boolean pinned = false;

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 1, max = 50, message = "Title must be between 1 and 50 characters")
    private String title;
}
