package ru.practicum.service;

import ru.practicum.dto.CommentAdminUpdateDto;
import ru.practicum.dto.CommentDto;
import ru.practicum.dto.NewCommentDto;
import ru.practicum.dto.UpdateCommentDto;

import java.util.List;

public interface CommentService {
    CommentDto createComment(Long userId, NewCommentDto newCommentDto);

    CommentDto updateCommentByUser(Long userId, Long commentId, UpdateCommentDto updateCommentDto);

    void deleteCommentByUser(Long userId, Long commentId);

    List<CommentDto> getCommentsForEvent(Long eventId, Integer from, Integer size);

    List<CommentDto> getCommentsForModeration(Integer from, Integer size);

    CommentDto moderateComment(Long commentId, CommentAdminUpdateDto adminUpdateDto);

    Long getPublishedCommentsCount(Long eventId);

    void deleteUserComments(Long userId);
}
