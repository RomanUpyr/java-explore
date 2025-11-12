package ru.practicum.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.*;
import ru.practicum.repository.CompilationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private BaseService baseService;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private User createTestUser(Long id) {
        return User.builder()
                .id(id)
                .name("User " + id)
                .email("user" + id + "@email.com")
                .build();
    }

    private Category createTestCategory(Long id) {
        return Category.builder()
                .id(id)
                .name("Category " + id)
                .build();
    }

    private Event createTestEvent(Long id) {
        return Event.builder()
                .id(id)
                .annotation("Event " + id)
                .category(createTestCategory(id))
                .description("Description " + id)
                .eventDate(LocalDateTime.now().plusDays(id))
                .initiator(createTestUser(id))
                .location(new Location(55.7558F, 37.6173F))
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .title("Event " + id)
                .createdOn(LocalDateTime.now())
                .confirmedRequests(0)
                .views(0L)
                .build();
    }

    private Compilation createTestCompilation(Long id, List<Event> events, Boolean pinned, String title) {
        return Compilation.builder()
                .id(id)
                .events(events)
                .pinned(pinned)
                .title(title)
                .build();
    }

    @Test
    void getCompilations_WithPinnedFilter_ShouldReturnFilteredCompilations() {
        // Given
        Event event1 = createTestEvent(1L);
        Event event2 = createTestEvent(2L);
        Compilation compilation = createTestCompilation(1L, List.of(event1, event2), true, "Pinned Compilation");

        when(compilationRepository.findByPinned(eq(true), any()))
                .thenReturn(List.of(compilation));
        when(baseService.createPageRequest(0, 10)).thenCallRealMethod();

        // When
        List<CompilationDto> result = compilationService.getCompilations(true, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Pinned Compilation", result.get(0).getTitle());
        assertTrue(result.get(0).getPinned());
    }

    @Test
    void getCompilation_WhenExists_ShouldReturnCompilation() {
        // Given
        Long compilationId = 1L;
        Event event = createTestEvent(1L);
        Compilation compilation = createTestCompilation(compilationId, List.of(event), true, "Test Compilation");

        when(compilationRepository.findById(compilationId)).thenReturn(Optional.of(compilation));

        // When
        CompilationDto result = compilationService.getCompilation(compilationId);

        // Then
        assertNotNull(result);
        assertEquals(compilationId, result.getId());
        assertEquals("Test Compilation", result.getTitle());
        assertEquals(1, result.getEvents().size());
    }

    @Test
    void getCompilation_WhenNotExists_ShouldThrowException() {
        // Given
        Long compilationId = 999L;

        when(compilationRepository.findById(compilationId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () ->
                compilationService.getCompilation(compilationId));
    }


    @Test
    void createCompilation_WithDuplicateTitle_ShouldThrowException() {
        // Given
        NewCompilationDto newCompilationDto = NewCompilationDto.builder()
                .title("Duplicate Compilation")
                .build();

        when(compilationRepository.existsByTitle("Duplicate Compilation")).thenReturn(true);

        // When & Then
        assertThrows(ConflictException.class, () ->
                compilationService.createCompilation(newCompilationDto));
    }

    @Test
    void createCompilation_WithEmptyEvents_ShouldCreateCompilation() {
        // Given
        NewCompilationDto newCompilationDto = NewCompilationDto.builder()
                .title("Empty Compilation")
                .events(null)
                .build();

        Compilation savedCompilation = createTestCompilation(1L, List.of(), false, "Empty Compilation");

        when(compilationRepository.existsByTitle("Empty Compilation")).thenReturn(false);
        when(compilationRepository.save(any(Compilation.class))).thenReturn(savedCompilation);

        // When
        CompilationDto result = compilationService.createCompilation(newCompilationDto);

        // Then
        assertNotNull(result);
        assertEquals("Empty Compilation", result.getTitle());
        assertTrue(result.getEvents().isEmpty());
    }


    @Test
    void updateCompilation_WithDuplicateTitle_ShouldThrowException() {
        // Given
        Long compilationId = 1L;
        Compilation existingCompilation = createTestCompilation(compilationId, List.of(), false, "Old Title");
        Compilation otherCompilation = createTestCompilation(2L, List.of(), false, "Existing Title");

        UpdateCompilationRequest updateRequest = UpdateCompilationRequest.builder()
                .title("Existing Title")
                .build();

        when(compilationRepository.findById(compilationId)).thenReturn(Optional.of(existingCompilation));
        when(compilationRepository.findByTitle("Existing Title")).thenReturn(Optional.of(otherCompilation));

        // When & Then
        assertThrows(ConflictException.class, () ->
                compilationService.updateCompilation(compilationId, updateRequest));
    }

    @Test
    void deleteCompilation_WhenExists_ShouldDeleteCompilation() {
        // Given
        Long compilationId = 1L;

        when(compilationRepository.existsById(compilationId)).thenReturn(true);
        doNothing().when(compilationRepository).deleteById(compilationId);

        // When
        compilationService.deleteCompilation(compilationId);

        // Then
        verify(compilationRepository).deleteById(compilationId);
    }

    @Test
    void deleteCompilation_WhenNotExists_ShouldThrowException() {
        // Given
        Long compilationId = 999L;

        when(compilationRepository.existsById(compilationId)).thenReturn(false);

        // When & Then
        assertThrows(NotFoundException.class, () ->
                compilationService.deleteCompilation(compilationId));
    }
}