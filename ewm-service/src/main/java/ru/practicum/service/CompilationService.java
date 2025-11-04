package ru.practicum.service;

import ru.practicum.dto.CompilationDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    List<CompilationDto> getCompilations(Boolean pinned, int from, int size);

    CompilationDto getCompilation(Long compilationId);

    CompilationDto createCompilation(NewCompilationDto newCompilationDto);

    void deleteCompilation(Long compilationId);

    CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest updateRequest);
}