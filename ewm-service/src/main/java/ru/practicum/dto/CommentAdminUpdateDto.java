package ru.practicum.dto;

import lombok.*;
import ru.practicum.model.CommentStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentAdminUpdateDto {
    private CommentStatus status;
}
