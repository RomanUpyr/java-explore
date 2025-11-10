package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.Category;

/**
 * Репозиторий для работы с категориями событий.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {
    /**
     * Проверяет существование категории с указанным именем.
     */
    boolean existsByName(String name);

    /**
     * Проверяет существование категории с указанным именем, исключая категорию с заданным ID.
     */
    boolean existsByNameAndIdNot(String name, Long id);
}
