package ru.practicum.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.dto.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.model.*;
import ru.practicum.repository.CommentRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User createTestUser(Long id) {
        return User.builder()
                .id(id)
                .name("Test User")
                .email("test@email.com")
                .build();
    }

    private Event createTestEvent(Long id, EventState state) {
        return Event.builder()
                .id(id)
                .annotation("Test Event")
                .state(state)
                .build();
    }

    private Comment createTestComment(Long id, User author, Event event, CommentStatus status) {
        return Comment.builder()
                .id(id)
                .text("Test comment")
                .author(author)
                .event(event)
                .status(status)
                .createdOn(LocalDateTime.now())
                .build();
    }

    @Test
    void createComment_WithValidData_ShouldCreateComment() {
        // Given
        Long userId = 1L;
        Long eventId = 1L;
        User user = createTestUser(userId);
        Event event = createTestEvent(eventId, EventState.PUBLISHED);
        NewCommentDto newCommentDto = NewCommentDto.builder()
                .text("New comment")
                .eventId(eventId)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(commentRepository.existsByAuthorIdAndEventId(userId, eventId)).thenReturn(false);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(1L);
            return comment;
        });

        // When
        CommentDto result = commentService.createComment(userId, newCommentDto);

        // Then
        assertNotNull(result);
        assertEquals("New comment", result.getText());
        assertEquals(CommentStatus.PENDING, result.getStatus());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createComment_WhenEventNotPublished_ShouldThrowException() {
        // Given
        Long userId = 1L;
        Long eventId = 1L;
        User user = createTestUser(userId);
        Event event = createTestEvent(eventId, EventState.PENDING);
        NewCommentDto newCommentDto = NewCommentDto.builder()
                .text("New comment")
                .eventId(eventId)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When & Then
        assertThrows(ConflictException.class, () ->
                commentService.createComment(userId, newCommentDto));
    }

    @Test
    void updateCommentByUser_WithValidData_ShouldUpdateComment() {
        // Given
        Long userId = 1L;
        Long commentId = 1L;
        User user = createTestUser(userId);
        Event event = createTestEvent(1L, EventState.PUBLISHED);
        Comment comment = createTestComment(commentId, user, event, CommentStatus.PENDING);
        UpdateCommentDto updateDto = UpdateCommentDto.builder()
                .text("Updated comment")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        CommentDto result = commentService.updateCommentByUser(userId, commentId, updateDto);

        // Then
        assertNotNull(result);
        assertEquals(CommentStatus.PENDING, result.getStatus());
        verify(commentRepository).save(comment);
    }

    @Test
    void getCommentsForEvent_ShouldReturnPublishedComments() {
        // Given
        Long eventId = 1L;
        User user = createTestUser(1L);
        Event event = createTestEvent(eventId, EventState.PUBLISHED);
        Comment comment = createTestComment(1L, user, event, CommentStatus.PUBLISHED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(commentRepository.findByEventIdAndStatus(eq(eventId), eq(CommentStatus.PUBLISHED), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(comment)));

        // When
        List<CommentDto> result = commentService.getCommentsForEvent(eventId, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CommentStatus.PUBLISHED, result.get(0).getStatus());
    }

    @Test
    void moderateComment_ShouldUpdateStatus() {
        // Given
        Long commentId = 1L;
        User user = createTestUser(1L);
        Event event = createTestEvent(1L, EventState.PUBLISHED);
        Comment comment = createTestComment(commentId, user, event, CommentStatus.PENDING);
        CommentAdminUpdateDto updateDto = CommentAdminUpdateDto.builder()
                .status(CommentStatus.PUBLISHED)
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        CommentDto result = commentService.moderateComment(commentId, updateDto);

        // Then
        assertNotNull(result);
        assertEquals(CommentStatus.PUBLISHED, result.getStatus());
    }
}