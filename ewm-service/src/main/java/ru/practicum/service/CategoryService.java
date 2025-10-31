package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.CategoryDto;
import ru.practicum.dto.NewCategoryDto;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.exception.ConflictException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с категориями
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService extends BaseService {
    /**
     * Получение всех категорий
     */
    public List<CategoryDto> getCategories(int from, int size) {
        log.info("Getting categories: from={}, size={}", from, size);

        return categoryRepository.findAll(createPageRequest(from, size))
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение категории по ID
     */
    public CategoryDto getCategory(Long categoryId) {
        log.info("Getting category id={}", categoryId);

        Category category = getCategoryById(categoryId);
        return convertToDto(category);
    }

    /**
     * Создание категории
     */
    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        log.info("Creating category: {}", newCategoryDto);

        // Проверяем уникальность имени
        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new ConflictException("Category with name=" + newCategoryDto.getName() + " already exists");
        }

        Category category = Category.builder()
                .name(newCategoryDto.getName())
                .build();

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created with id={}", savedCategory.getId());

        return convertToDto(savedCategory);
    }

    /**
     * Обновление категории
     */
    @Transactional
    public CategoryDto updateCategory(Long categoryId, CategoryDto categoryDto) {
        log.info("Updating category id={}: {}", categoryId, categoryDto);

        Category category = getCategoryById(categoryId);

        // Проверяем уникальность имени
        if (categoryRepository.existsByNameAndIdNot(categoryDto.getName(), categoryId)) {
            throw new ConflictException("Category with name=" + categoryDto.getName() + " already exists");
        }
        category.setName(categoryDto.getName());
        Category updatedCategory = categoryRepository.save(category);

        return convertToDto(updatedCategory);
    }

    /**
     * Удаление категории
     */
    @Transactional
    public void deleteCategory(Long categoryId) {
        log.info("Deleting category id={}", categoryId);

        Category category = getCategoryById(categoryId);

        // Проверяем, что нет связанных событий
        List<Event> events = eventRepository.findByCategoryId(categoryId);
        if (!events.isEmpty()) {
            throw new ConflictException("The category is not empty");
        }

        categoryRepository.delete(category);
        log.info("Category id={} deleted", categoryId);
    }

    private CategoryDto convertToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
