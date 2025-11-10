package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.Compilation;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с подборками событий.
 */
public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    /**
     * Находит подборки событий с учетом признака закрепления.
     */
    List<Compilation> findByPinned(Boolean pinned, Pageable pageable);

    /**
     * Проверяет существование подборки с указанным заголовком.
     */
    boolean existsByTitle(String title);

    /**
     * Находит подборку по заголовку.
     */
    Optional<Compilation> findByTitle(String title);
}
