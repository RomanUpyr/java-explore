package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Comment;
import ru.practicum.model.CommentStatus;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByEventIdAndStatus(Long eventId, CommentStatus status, Pageable pageable);

    Page<Comment> findByStatus(CommentStatus status, Pageable pageable);

    List<Comment> findByAuthorId(Long authorId);

    Optional<Comment> findByIdAndAuthorId(Long commentId, Long authorId);

    List<Comment> findByEventId(Long eventId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.event.id = :eventId AND c.status = 'PUBLISHED'")
    Long countPublishedCommentsByEventId(@Param("eventId") Long eventId);

    boolean existsByAuthorIdAndEventId(Long authorId, Long eventId);

    void deleteByAuthorId(Long authorId);

    void deleteByEventId(Long eventId);
}
