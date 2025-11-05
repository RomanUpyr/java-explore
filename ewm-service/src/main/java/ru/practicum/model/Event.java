package ru.practicum.model;

import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Сущность события.
 */
@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    /**
     * Уникальный идентификатор.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Краткое описание события.
     */
    @Column(nullable = false, length = 2000)
    private String annotation;

    /**
     * Категория события.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Количество подтвержденных заявок на участие.
     */
    @Column(name = "confirmed_requests")
    @Builder.Default
    private Integer confirmedRequests = 0;

    /**
     * Дата и время создания события.
     */
    @Column(name = "created_on")
    private LocalDateTime createdOn;

    /**
     * Полное описание события.
     */
    @Column(length = 7000)
    private String description;

    /**
     * Дата и время проведения события.
     */
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    /**
     * Пользователь, создавший событие.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    /**
     * Координаты места проведения события.
     */
    @Embedded
    private Location location;

    /**
     * Флаг платности события (true - платное).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean paid = false;

    /**
     * Ограничение количества участников (0 - без ограничений).
     */
    @Column(name = "participant_limit")
    @Builder.Default
    private Integer participantLimit = 0;

    /**
     * Дата и время публикации события.
     */
    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    /**
     * Требуется ли модерация заявок (true - требуется).
     */
    @Column(name = "request_moderation")
    @Builder.Default
    private Boolean requestModeration = true;

    /**
     * Текущее состояние события.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EventState state = EventState.PENDING;

    /**
     * Заголовок события.
     */
    @Column(nullable = false, length = 120)
    private String title;

    // рассчитывается на основе статистики, не сохраняется в БД
    @Transient
    @Builder.Default
    private Long views = 0L;
}
