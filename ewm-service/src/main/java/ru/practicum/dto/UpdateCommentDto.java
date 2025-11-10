package ru.practicum.dto;

import lombok.*;

import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCommentDto {
    @Size(min = 1, max = 2000, message = "Длина комментария должна быть от 1 до 2000 символов")
    private String text;
}
