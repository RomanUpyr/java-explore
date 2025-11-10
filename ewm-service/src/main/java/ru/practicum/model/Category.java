package ru.practicum.model;

import lombok.*;
import jakarta.persistence.*;

/**
 * Сущность категории событий.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    /**
     * Уникальный идентификатор.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название категории.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
