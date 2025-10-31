package ru.practicum.model;

import lombok.*;
import jakarta.persistence.*;

import java.util.List;

/**
 * Сущность подборки событий
 */
@Entity
@Table(name = "compilations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compilation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany
    @JoinTable(
            name = "compilation_events",
            joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    private List<Event> events;

    @Column(nullable = false)
    private Boolean pinned = false;

    @Column(nullable = false, length = 50)
    private String title;
}
