package ru.practicum.dto;

import lombok.*;
import jakarta.validation.constraints.*;

/**
 * DTO для пользователя системы
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;            // Уникальный идентификатор пользователя

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 250, message = "Name must be between 2 and 250 characters")
    private String name;        // Имя пользователя

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    @Size(min = 6, max = 254, message = "Email must be between 6 and 254 characters")
    private String email;       // Email пользователя
}
