package ru.practicum.model;

import lombok.*;
import jakarta.persistence.*;

import java.util.List;

/**
 * Сущность подборки событий
 * Подборки позволяют группировать события по тематике или другим критериям
 */
@Entity
@Table(name = "compilations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compilation {
    /**
     * Уникальный идентификатор.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Список событий в подборке.
     */
    @ManyToMany
    @JoinTable(
            name = "compilation_events",
            joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    private List<Event> events;

    /**
     * Флаг закрепления подборки.
     */
    @Column(nullable = false)
    private Boolean pinned = false;

    /**
     * Заголовок подборки.
     */
    @Column(nullable = false, length = 50)
    private String title;
}
