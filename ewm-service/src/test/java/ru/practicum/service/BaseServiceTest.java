package ru.practicum.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.*;
import ru.practicum.repository.*;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private StatsTrackingService statsTrackingService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private BaseService baseService;

    @Test
    void getUserById_WhenUserExists_ShouldReturnUser() {
        // Given
        Long userId = 1L;
        User user = User.builder().id(userId).name("Test User").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        User result = baseService.getUserById(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    void getUserById_WhenUserNotExists_ShouldThrowException() {
        // Given
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> baseService.getUserById(userId));
    }

    @Test
    void getCategoryById_WhenCategoryExists_ShouldReturnCategory() {
        // Given
        Long categoryId = 1L;
        Category category = Category.builder().id(categoryId).name("Test Category").build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // When
        Category result = baseService.getCategoryById(categoryId);

        // Then
        assertNotNull(result);
        assertEquals(categoryId, result.getId());
    }

    @Test
    void getEventById_WhenEventExists_ShouldReturnEvent() {
        // Given
        Long eventId = 1L;
        Event event = Event.builder().id(eventId).title("Test Event").build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When
        Event result = baseService.getEventById(eventId);

        // Then
        assertNotNull(result);
        assertEquals(eventId, result.getId());
    }

    @Test
    void getEventByIdAndInitiatorId_WhenEventExists_ShouldReturnEvent() {
        // Given
        Long eventId = 1L;
        Long userId = 1L;
        Event event = Event.builder().id(eventId).title("Test Event").build();

        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));

        // When
        Event result = baseService.getEventByIdAndInitiatorId(eventId, userId);

        // Then
        assertNotNull(result);
        assertEquals(eventId, result.getId());
    }

    @Test
    void parseDateTime_ShouldParseValidDateTime() {
        // Given
        String dateTimeStr = "2024-12-31 12:00:00";
        LocalDateTime expected = LocalDateTime.of(2024, 12, 31, 12, 0, 0);

        when(statsTrackingService.parseDateTime(dateTimeStr)).thenReturn(expected);

        // When
        LocalDateTime result = baseService.parseDateTime(dateTimeStr);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void formatDateTime_ShouldFormatDateTime() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 31, 12, 0, 0);
        String expected = "2024-12-31 12:00:00";

        when(statsTrackingService.formatDateTime(dateTime)).thenReturn(expected);

        // When
        String result = baseService.formatDateTime(dateTime);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void sendStats_WithUriAndIp_ShouldCallTrackingService() {
        // Given
        String uri = "/events/1";
        String clientIp = "192.168.1.1";

        doNothing().when(statsTrackingService).trackHit(uri, clientIp);

        // When
        baseService.sendStats(clientIp, uri);

        // Then
        verify(statsTrackingService).trackHit(uri, clientIp);
    }

    @Test
    void sendStats_WithRequest_ShouldCallTrackingService() {
        // Given
        doNothing().when(statsTrackingService).trackHit(request);

        // When
        baseService.sendStats(request);

        // Then
        verify(statsTrackingService).trackHit(request);
    }

    @Test
    void getClientIp_ShouldDelegateToTrackingService() {
        // Given
        String expectedIp = "192.168.1.1";
        when(statsTrackingService.getClientIp(request)).thenReturn(expectedIp);

        // When
        String result = baseService.getClientIp(request);

        // Then
        assertEquals(expectedIp, result);
    }

    @Test
    void userExistsByEmail_ShouldReturnTrueWhenExists() {
        // Given
        String email = "test@email.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // When
        boolean result = baseService.userExistsByEmail(email);

        // Then
        assertTrue(result);
    }

    @Test
    void categoryExistsByName_ShouldReturnTrueWhenExists() {
        // Given
        String name = "Test Category";
        when(categoryRepository.existsByName(name)).thenReturn(true);

        // When
        boolean result = baseService.categoryExistsByName(name);

        // Then
        assertTrue(result);
    }

    @Test
    void requestExistsByEventIdAndRequesterId_ShouldReturnTrueWhenExists() {
        // Given
        Long eventId = 1L;
        Long requesterId = 1L;
        when(requestRepository.existsByEventIdAndRequesterId(eventId, requesterId)).thenReturn(true);

        // When
        boolean result = baseService.requestExistsByEventIdAndRequesterId(eventId, requesterId);

        // Then
        assertTrue(result);
    }

    @Test
    void getEventsByCategoryId_ShouldReturnEvents() {
        // Given
        Long categoryId = 1L;
        Event event = Event.builder().id(1L).title("Test Event").build();
        when(eventRepository.findByCategoryId(categoryId)).thenReturn(List.of(event));

        // When
        List<Event> result = baseService.getEventsByCategoryId(categoryId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Event", result.get(0).getTitle());
    }

    @Test
    void userExists_ShouldReturnTrueWhenUserExists() {
        // Given
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

        // When
        boolean result = baseService.userExists(userId);

        // Then
        assertTrue(result);
    }

    @Test
    void getEventViews_ShouldReturnViews() {
        // Given
        Long eventId = 1L;
        int expectedViews = 100;
        when(statsTrackingService.getEventViews(eventId)).thenReturn(expectedViews);

        // When
        int result = baseService.getEventViews(eventId);

        // Then
        assertEquals(expectedViews, result);
    }
}