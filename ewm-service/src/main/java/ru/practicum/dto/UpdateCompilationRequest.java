package ru.practicum.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для обновления подборки событий
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompilationRequest {
    private List<Long> events;          // Новый список событий
    private Boolean pinned;             // Новый флаг закрепления

    @Size(min = 1, max = 50, message = "Title must be between 1 and 50 characters")
    private String title;               // Новый заголовок
}
