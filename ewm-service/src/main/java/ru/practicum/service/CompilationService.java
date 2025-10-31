package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.*;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.Category;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.model.User;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.exception.ConflictException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с подборками событий
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationService extends BaseService{
    /**
     * Получение всех подборок
     */
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        log.info("Getting compilations: pinned={}, from={}, size={}", pinned, from, size);

        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findByPinned(pinned, createPageRequest(from, size));
        } else {
            compilations = compilationRepository.findAll(createPageRequest(from, size)).getContent();
        }

        return compilations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение подборки по ID
     */
    public CompilationDto getCompilation(Long compilationId) {
        log.info("Getting compilation id={}", compilationId);

        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compilationId + " was not found"));

        return convertToDto(compilation);
    }

    /**
     * Создание подборки
     */
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        log.info("Creating compilation: {}", newCompilationDto);

        if (compilationRepository.existsByTitle(newCompilationDto.getTitle())) {
            throw new ConflictException("Compilation with title=" + newCompilationDto.getTitle() + " already exists");
        }
        List<Event> events = new ArrayList<>();
        if (newCompilationDto.getEvents() != null) {
            events = eventRepository.findByIdIn(newCompilationDto.getEvents());
        }

        Compilation compilation = Compilation.builder()
                .events(events)
                .pinned(newCompilationDto.getPinned() != null ? newCompilationDto.getPinned() : false)
                .title(newCompilationDto.getTitle())
                .build();

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Compilation created with id={}", savedCompilation.getId());

        return convertToDto(savedCompilation);
    }

    /**
     * Удаление подборки
     */
    @Transactional
    public void deleteCompilation(Long compilationId) {
        log.info("Deleting compilation id={}", compilationId);

        if (!compilationRepository.existsById(compilationId)) {
            throw new NotFoundException("Compilation with id=" + compilationId + " was not found");
        }

        compilationRepository.deleteById(compilationId);
        log.info("Compilation id={} deleted", compilationId);
    }

    /**
     * Обновление подборки
     */
    public CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest updateRequest) {
        log.info("Updating compilation id={}: {}", compilationId, updateRequest);

        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compilationId + " was not found"));

        if (updateRequest.getTitle() != null) {
            compilationRepository.findByTitle(updateRequest.getTitle())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(compilationId)) {
                            throw new ConflictException("Compilation with title=" + updateRequest.getTitle() + " already exists");
                        }
                    });
            compilation.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getEvents() != null) {
            List<Event> events = eventRepository.findByIdIn(updateRequest.getEvents());
            compilation.setEvents(events);
        }

        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        return convertToDto(updatedCompilation);
    }

    private CompilationDto convertToDto(Compilation compilation) {
        List<EventShortDto> eventDtos = compilation.getEvents().stream()
                .map(event -> EventShortDto.builder()
                        .id(event.getId())
                        .annotation(event.getAnnotation())
                        .category(convertToCategoryDto(event.getCategory()))
                        .confirmedRequests(event.getConfirmedRequests())
                        .eventDate(formatDateTime(event.getEventDate()))
                        .initiator(convertToUserShortDto(event.getInitiator()))
                        .paid(event.getPaid())
                        .title(event.getTitle())
                        .views(event.getViews())
                        .build())
                .collect(Collectors.toList());

        return CompilationDto.builder()
                .id(compilation.getId())
                .events(eventDtos)
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }

    private CategoryDto convertToCategoryDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    private UserShortDto convertToUserShortDto(User user) {
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
