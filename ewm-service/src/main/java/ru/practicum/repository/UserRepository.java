package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.*;

/**
 * Репозиторий для работы с пользователями системы.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Проверяет существование пользователя с указанным email.
     */
    boolean existsByEmail(String email);

    /**
     * Проверяет существование пользователя с указанным email, исключая пользователя с заданным ID.
     */
    boolean existsByEmailAndIdNot(String email, Long id);
}
