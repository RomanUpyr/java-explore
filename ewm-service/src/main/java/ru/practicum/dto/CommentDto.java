package ru.practicum.dto;

import lombok.*;
import ru.practicum.model.CommentStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {
    private Long id;
    private String text;
    private Long eventId;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
    private CommentStatus status;

}
