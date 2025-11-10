package ru.practicum.dto;

import lombok.*;
import jakarta.validation.constraints.*;

/**
 * DTO для категории событий
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Long id;          // Уникальный идентификатор категории

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 1, max = 50, message = "Name must be between 1 and 50 characters")
    private String name;     // Название категории
}
