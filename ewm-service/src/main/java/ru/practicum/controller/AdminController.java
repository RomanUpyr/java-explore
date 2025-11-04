package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.*;
import ru.practicum.service.*;

import jakarta.validation.Valid;

import java.util.List;

/**
 * API для администраторов
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    private final CategoryService categoryService;
    private final EventService eventService;
    private final CompilationService compilationService;

    /**
     * Получение списка пользователей с возможностью фильтрации
     *
     * @param ids  список ID пользователей для фильтрации (опционально)
     * @param from начальная позиция (по умолчанию 0)
     * @param size количество элементов на странице (по умолчанию 10)
     * @return список пользователей в формате DTO
     */
    @GetMapping("/users")
    public List<UserDto> getUsers(@RequestParam(required = false) List<Long> ids,
                                  @RequestParam(defaultValue = "0") int from,
                                  @RequestParam(defaultValue = "10") int size) {
        return userService.getUsers(ids, from, size);
    }

    /**
     * Создание нового пользователя
     *
     * @param userDto данные нового пользователя
     * @return созданный пользователь в формате DTO
     */
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody UserDto userDto) {
        return userService.createUser(userDto);
    }

    /**
     * Удаление пользователя по ID
     *
     * @param userId ID пользователя для удаления
     */
    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
    }

    /**
     * Создание новой категории событий
     *
     * @param newCategoryDto данные новой категории
     * @return созданная категория в формате DTO
     */
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody NewCategoryDto newCategoryDto) {
        return categoryService.createCategory(newCategoryDto);
    }

    /**
     * Удаление категории по ID
     *
     * @param categoryId ID категории для удаления
     */
    @DeleteMapping("/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
    }

    /**
     * Обновление данных категории
     *
     * @param categoryId  ID категории для обновления
     * @param categoryDto новые данные категории
     * @return обновленная категория в формате DTO
     */
    @PatchMapping("/categories/{categoryId}")
    public CategoryDto updateCategory(@PathVariable Long categoryId,
                                      @Valid @RequestBody CategoryDto categoryDto) {
        return categoryService.updateCategory(categoryId, categoryDto);
    }

    /**
     * Получение событий с расширенной фильтрацией для администраторов
     *
     * @param users      список ID пользователей-организаторов (опционально)
     * @param states     список статусов событий (опционально)
     * @param categories список ID категорий (опционально)
     * @param rangeStart начальная дата диапазона (опционально)
     * @param rangeEnd   конечная дата диапазона (опционально)
     * @param from       начальная позиция
     * @param size       количество элементов на странице
     * @return список событий с полной информацией
     */
    @GetMapping("/events")
    public List<EventFullDto> getEventsForAdmin(@RequestParam(required = false) List<Long> users,
                                                @RequestParam(required = false) List<String> states,
                                                @RequestParam(required = false) List<Long> categories,
                                                @RequestParam(required = false) String rangeStart,
                                                @RequestParam(required = false) String rangeEnd,
                                                @RequestParam(defaultValue = "0") int from,
                                                @RequestParam(defaultValue = "10") int size) {
        return eventService.getEventsForAdmin(users, states, categories, rangeStart, rangeEnd, from, size);
    }

    /**
     * Обновление события администратором
     *
     * @param eventId       ID события для обновления
     * @param updateRequest запрос на обновление с новыми данными
     * @return обновленное событие с полной информацией
     */
    @PatchMapping("/events/{eventId}")
    public EventFullDto updateEventByAdmin(@PathVariable Long eventId,
                                           @Valid @RequestBody UpdateEventAdminRequest updateRequest) {
        return eventService.updateEventByAdmin(eventId, updateRequest);
    }

    /**
     * Создание новой подборки событий
     *
     * @param newCompilationDto данные новой подборки
     * @return созданная подборка в формате DTO
     */
    @PostMapping("/compilations")
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto createCompilation(@Valid @RequestBody NewCompilationDto newCompilationDto) {
        return compilationService.createCompilation(newCompilationDto);
    }

    /**
     * Удаление подборки по ID
     *
     * @param compilationId ID подборки для удаления
     */
    @DeleteMapping("/compilations/{compilationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompilation(@PathVariable Long compilationId) {
        compilationService.deleteCompilation(compilationId);
    }

    /**
     * Обновление данных подборки
     *
     * @param compilationId ID подборки для обновления
     * @param updateRequest запрос на обновление с новыми данными
     * @return обновленная подборка в формате DTO
     */
    @PatchMapping("/compilations/{compilationId}")
    public CompilationDto updateCompilation(@PathVariable Long compilationId,
                                            @Valid @RequestBody UpdateCompilationRequest updateRequest) {
        return compilationService.updateCompilation(compilationId, updateRequest);
    }
}
