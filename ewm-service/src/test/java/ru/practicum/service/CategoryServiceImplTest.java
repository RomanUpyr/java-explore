package ru.practicum.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.dto.CategoryDto;
import ru.practicum.dto.NewCategoryDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.Category;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private BaseService baseService;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category createTestCategory(Long id, String name) {
        return Category.builder()
                .id(id)
                .name(name)
                .build();
    }

    @Test
    void getCategories_ShouldReturnAllCategories() {
        // Given
        Category category1 = createTestCategory(1L, "Category 1");
        Category category2 = createTestCategory(2L, "Category 2");
        Page<Category> categoryPage = new PageImpl<>(List.of(category1, category2));
        PageRequest pageRequest = PageRequest.of(0, 10);

        when(baseService.createPageRequest(0, 10)).thenReturn(pageRequest);
        when(categoryRepository.findAll(pageRequest)).thenReturn(categoryPage);

        // When
        List<CategoryDto> result = categoryService.getCategories(0, 10);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Category 1", result.get(0).getName());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Category 2", result.get(1).getName());
        assertEquals(2L, result.get(1).getId());

        verify(baseService).createPageRequest(0, 10);
        verify(categoryRepository).findAll(pageRequest);
    }


    @Test
    void getCategory_WhenCategoryExists_ShouldReturnCategory() {
        // Given
        Long categoryId = 1L;
        Category category = createTestCategory(categoryId, "Test Category");

        when(baseService.getCategoryById(categoryId)).thenReturn(category);

        // When
        CategoryDto result = categoryService.getCategory(categoryId);

        // Then
        assertNotNull(result);
        assertEquals(categoryId, result.getId());
        assertEquals("Test Category", result.getName());
    }

    @Test
    void getCategory_WhenCategoryNotExists_ShouldThrowException() {
        // Given
        Long categoryId = 999L;
        when(baseService.getCategoryById(categoryId))
                .thenThrow(new NotFoundException("Category not found"));

        // When & Then
        assertThrows(NotFoundException.class, () ->
                categoryService.getCategory(categoryId));
    }

    @Test
    void createCategory_WithUniqueName_ShouldCreateCategory() {
        // Given
        NewCategoryDto newCategoryDto = NewCategoryDto.builder()
                .name("New Category")
                .build();
        Category savedCategory = createTestCategory(1L, "New Category");

        when(categoryRepository.existsByName("New Category")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // When
        CategoryDto result = categoryService.createCategory(newCategoryDto);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("New Category", result.getName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_WithDuplicateName_ShouldThrowException() {
        // Given
        NewCategoryDto newCategoryDto = NewCategoryDto.builder()
                .name("Existing Category")
                .build();

        when(categoryRepository.existsByName("Existing Category")).thenReturn(true);

        // When & Then
        assertThrows(ConflictException.class, () ->
                categoryService.createCategory(newCategoryDto));
    }

    @Test
    void updateCategory_WithValidData_ShouldUpdateCategory() {
        // Given
        Long categoryId = 1L;
        CategoryDto updateDto = CategoryDto.builder()
                .id(categoryId)
                .name("Updated Category")
                .build();
        Category existingCategory = createTestCategory(categoryId, "Old Category");

        when(baseService.getCategoryById(categoryId)).thenReturn(existingCategory);
        when(categoryRepository.existsByNameAndIdNot("Updated Category", categoryId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(existingCategory);

        // When
        CategoryDto result = categoryService.updateCategory(categoryId, updateDto);

        // Then
        assertNotNull(result);
        assertEquals("Updated Category", result.getName());
        verify(categoryRepository).save(existingCategory);
    }

    @Test
    void updateCategory_WithDuplicateName_ShouldThrowException() {
        // Given
        Long categoryId = 1L;
        CategoryDto updateDto = CategoryDto.builder()
                .id(categoryId)
                .name("Duplicate Category")
                .build();
        Category existingCategory = createTestCategory(categoryId, "Old Category");

        when(baseService.getCategoryById(categoryId)).thenReturn(existingCategory);
        when(categoryRepository.existsByNameAndIdNot("Duplicate Category", categoryId)).thenReturn(true);

        // When & Then
        assertThrows(ConflictException.class, () ->
                categoryService.updateCategory(categoryId, updateDto));
    }

    @Test
    void deleteCategory_WhenNoEvents_ShouldDeleteCategory() {
        // Given
        Long categoryId = 1L;
        Category category = createTestCategory(categoryId, "Test Category");

        when(baseService.getCategoryById(categoryId)).thenReturn(category);
        when(eventRepository.existsByCategoryId(categoryId)).thenReturn(false);
        doNothing().when(categoryRepository).delete(category);

        // When
        categoryService.deleteCategory(categoryId);

        // Then
        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_WhenCategoryHasEvents_ShouldThrowException() {
        // Given
        Long categoryId = 1L;
        Category category = createTestCategory(categoryId, "Test Category");

        when(baseService.getCategoryById(categoryId)).thenReturn(category);
        when(eventRepository.existsByCategoryId(categoryId)).thenReturn(true);

        // When & Then
        assertThrows(ConflictException.class, () ->
                categoryService.deleteCategory(categoryId));
    }
}