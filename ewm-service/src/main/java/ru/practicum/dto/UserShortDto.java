package ru.practicum.dto;

import lombok.*;

/**
 * DTO для краткой информации о пользователе
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserShortDto {
    private Long id;          // Уникальный идентификатор пользователя
    private String name;      // Имя пользователя
}
