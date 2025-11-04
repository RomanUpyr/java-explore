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
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final BaseService baseService;

    /**
     * Получение всех категорий
     */
    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        log.debug("Getting categories: from={}, size={}", from, size);

        return categoryRepository.findAll(baseService.createPageRequest(from, size))
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение категории по ID
     */
    @Override
    public CategoryDto getCategory(Long categoryId) {
        log.debug("Getting category id={}", categoryId);

        Category category = baseService.getCategoryById(categoryId);
        return convertToDto(category);
    }

    /**
     * Создание категории
     */
    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        log.debug("Creating category: {}", newCategoryDto);

        // Проверяем уникальность имени
        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new ConflictException("Category with name=" + newCategoryDto.getName() + " already exists");
        }

        Category category = Category.builder()
                .name(newCategoryDto.getName())
                .build();

        Category savedCategory = categoryRepository.save(category);
        log.debug("Category created with id={}", savedCategory.getId());

        return convertToDto(savedCategory);
    }

    /**
     * Обновление категории
     */
    @Override
    @Transactional
    public CategoryDto updateCategory(Long categoryId, CategoryDto categoryDto) {
        log.debug("Updating category id={}: {}", categoryId, categoryDto);

        Category category = baseService.getCategoryById(categoryId);

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
    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        log.debug("Deleting category id={}", categoryId);

        Category category = baseService.getCategoryById(categoryId);

        // Проверяем, что нет связанных событий
        List<Event> events = eventRepository.findByCategoryId(categoryId);
        if (!events.isEmpty()) {
            throw new ConflictException("The category is not empty");
        }

        categoryRepository.delete(category);
        log.debug("Category id={} deleted", categoryId);
    }

    private CategoryDto convertToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
