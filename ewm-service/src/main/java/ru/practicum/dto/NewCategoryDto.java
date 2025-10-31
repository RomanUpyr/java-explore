package ru.practicum.dto;

import lombok.*;
import jakarta.validation.constraints.*;

/**
 * DTO для создания новой категории
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCategoryDto {
    @NotBlank(message = "Name cannot be blank")
    @Size(min = 1, max = 50, message = "Name must be between 1 and 50 characters")
    private String name;
}
