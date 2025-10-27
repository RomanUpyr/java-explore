package ru.practicum.model;

import lombok.*;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * JPA сущность для хранения информации о посещениях эндпоинтов.
 */
@Entity
@Table(name = "endpoint_hits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointHit {

    /**
     * Уникальный идентификатор записи.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Идентификатор сервиса.
     */
    @Column(name = "app", nullable = false)
    private String app;

    /**
     * URI который посетили.
     */
    @Column(name = "uri", nullable = false)
    private String uri;

    /**
     * IP-адрес пользователя.
     */
    @Column(name = "ip", nullable = false)
    private String ip;

    /**
     * Дата и время посещения.
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
