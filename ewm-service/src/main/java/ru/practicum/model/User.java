package ru.practicum.model;

import lombok.*;
import jakarta.persistence.*;

/**
 * Сущность пользователя.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    /**
     * Уникальный идентификатор.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Имя пользователя.
     */
    @Column(nullable = false, length = 250)
    private String name;

    /**
     * Email пользователя.
     */
    @Column(nullable = false, unique = true, length = 254)
    private String email;
}
