package ru.practicum.model;

import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Сущность заявки на участие в событии.
 * Представляет запрос пользователя на участие в конкретном событии.
 */
@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationRequest {
    /**
     * Уникальный идентификатор.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Дата и время создания заявки.
     */
    @Column(name = "created")
    private LocalDateTime created;

    /**
     * Событие, на которое подана заявка.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    /**
     * Пользователь, подавший заявку.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private User requester;

    /**
     * Текущий статус заявки.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;
}
