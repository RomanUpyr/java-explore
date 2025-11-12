package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.CommentAdminUpdateDto;
import ru.practicum.dto.CommentDto;
import ru.practicum.dto.NewCommentDto;
import ru.practicum.dto.UpdateCommentDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.*;
import ru.practicum.repository.CommentRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, NewCommentDto newCommentDto) {
        User author = getUserById(userId);
        Event event = getEventById(newCommentDto.getEventId());

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя комментировать неопубликованное событие");
        }

        if (commentRepository.existsByAuthorIdAndEventId(userId, event.getId())) {
            throw new ConflictException("Вы уже оставляли комментарий к этому событию");
        }

        Comment comment = Comment.builder()
                .text(newCommentDto.getText())
                .event(event)
                .author(author)
                .createdOn(LocalDateTime.now())
                .status(CommentStatus.PENDING)
                .build();

        Comment savedComment = commentRepository.save(comment);
        log.debug("Создан новый комментарий ID: {} пользователем ID: {} к событию ID: {}",
                savedComment.getId(), userId, event.getId());

        return toCommentDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto updateCommentByUser(Long userId, Long commentId, UpdateCommentDto updateCommentDto) {

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с ID=" + userId + " не найден");
        }
        Comment comment = getCommentById(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("Комментарий не найден или у вас нет прав на его редактирование");
        }

        if (comment.getStatus() == CommentStatus.REJECTED) {
            throw new ConflictException("Нельзя редактировать отклоненный комментарий");
        }

        if (updateCommentDto.getText() != null && !updateCommentDto.getText().isBlank()) {
            comment.setText(updateCommentDto.getText());
        }

        comment.setUpdatedOn(LocalDateTime.now());
        comment.setStatus(CommentStatus.PENDING);

        Comment updatedComment = commentRepository.save(comment);
        log.debug("Обновлен комментарий ID: {} пользователем ID: {}", commentId, userId);

        return toCommentDto(updatedComment);
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с ID=" + userId + " не найден");
        }

        Comment comment = getCommentById(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("Комментарий не найден или у вас нет прав на его удаление");
        }

        commentRepository.delete(comment);
        log.debug("Удален комментарий ID: {} пользователем ID: {}", commentId, userId);
    }

    @Override
    public List<CommentDto> getCommentsForEvent(Long eventId, Integer from, Integer size) {

        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Событие с ID=" + eventId + " не найдено");
        }

        Pageable pageable = PageRequest.of(from / size, size);

        List<Comment> comments = commentRepository
                .findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED, pageable)
                .getContent();

        log.debug("Получено {} опубликованных комментариев для события ID: {}", comments.size(), eventId);
        return comments.stream()
                .map(this::toCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getCommentsForModeration(Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);

        List<Comment> comments = commentRepository
                .findByStatus(CommentStatus.PENDING, pageable)
                .getContent();

        log.debug("Получено {} комментариев для модерации", comments.size());
        return comments.stream()
                .map(this::toCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto moderateComment(Long commentId, CommentAdminUpdateDto adminUpdateDto) {
        Comment comment = getCommentById(commentId);

        if (adminUpdateDto.getStatus() != null) {
            comment.setStatus(adminUpdateDto.getStatus());
            comment.setUpdatedOn(LocalDateTime.now());
        }

        Comment moderatedComment = commentRepository.save(comment);
        log.debug("Комментарий ID: {} промодерирован со статусом: {}", commentId, adminUpdateDto.getStatus());

        return toCommentDto(moderatedComment);
    }

    @Override
    public Long getPublishedCommentsCount(Long eventId) {
        return commentRepository.countPublishedCommentsByEventId(eventId);
    }

    @Transactional
    public void deleteUserComments(Long userId) {
        commentRepository.deleteByAuthorId(userId);
        log.debug("Удалены все комментарии пользователя ID: {}", userId);
    }

    @Transactional
    public void deleteEventComments(Long eventId) {
        commentRepository.deleteByEventId(eventId);
        log.debug("Удалены все комментарии события ID: {}", eventId);
    }

    private Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с ID=" + commentId + " не найден"));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID=" + userId + " не найден"));
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID=" + eventId + " не найден"));
    }

    private CommentDto toCommentDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .eventId(comment.getEvent().getId())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getName())
                .createdOn(comment.getCreatedOn())
                .updatedOn(comment.getUpdatedOn())
                .status(comment.getStatus())
                .build();
    }
}
