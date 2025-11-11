package ru.practicum.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.*;
import ru.practicum.exception.*;
import ru.practicum.model.*;
import ru.practicum.repository.EventRepository;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private BaseService baseService;

    @Mock
    private StatsTrackingService statsTrackingService;

    @Mock
    private CommentService commentService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private EventServiceImpl eventService;

    private User createTestUser(Long id) {
        return User.builder()
                .id(id)
                .name("Test User")
                .email("test@email.com")
                .build();
    }

    private Category createTestCategory(Long id) {
        return Category.builder()
                .id(id)
                .name("Test Category")
                .build();
    }

    private Event createTestEvent(Long id, User initiator, Category category, EventState state) {
        return Event.builder()
                .id(id)
                .annotation("Test Annotation")
                .category(category)
                .description("Test Description")
                .eventDate(LocalDateTime.now().plusDays(1))
                .initiator(initiator)
                .location(new Location(55.7558F, 37.6173F))
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(state)
                .title("Test Event")
                .createdOn(LocalDateTime.now())
                .confirmedRequests(0)
                .views(0L)
                .build();
    }

    @Test
    void getEventsByUser_WhenUserNotExists_ShouldThrowException() {
        // Given
        Long userId = 1L;
        when(baseService.userExists(userId)).thenReturn(false);

        // When & Then
        assertThrows(NotFoundException.class, () ->
                eventService.getEventsByUser(userId, 0, 10));
    }

    @Test
    void createEvent_WithValidData_ShouldCreateEvent() {
        // Given
        Long userId = 1L;
        User user = createTestUser(userId);
        Category category = createTestCategory(1L);
        NewEventDto newEventDto = NewEventDto.builder()
                .annotation("Test Annotation")
                .category(1L)
                .description("Test Description")
                .eventDate(LocalDateTime.now().plusDays(2).toString())
                .location(new Location(55.7558F, 37.6173F))
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .title("Test Event")
                .build();

        when(baseService.getUserById(userId)).thenReturn(user);
        when(baseService.getCategoryById(1L)).thenReturn(category);
        when(baseService.parseDateTime(anyString())).thenReturn(LocalDateTime.now().plusDays(2));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(1L);
            return event;
        });

        // When
        EventFullDto result = eventService.createEvent(userId, newEventDto);

        // Then
        assertNotNull(result);
        assertEquals(newEventDto.getTitle(), result.getTitle());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createEvent_WithEventDateTooSoon_ShouldThrowException() {
        // Given
        Long userId = 1L;
        User user = createTestUser(userId);
        Category category = createTestCategory(1L);
        NewEventDto newEventDto = NewEventDto.builder()
                .annotation("Test Annotation")
                .category(1L)
                .description("Test Description")
                .eventDate(LocalDateTime.now().plusHours(1).toString())
                .location(new Location(55.7558F, 37.6173F))
                .title("Test Event")
                .build();

        when(baseService.getUserById(userId)).thenReturn(user);
        when(baseService.getCategoryById(1L)).thenReturn(category);
        when(baseService.parseDateTime(anyString())).thenReturn(LocalDateTime.now().plusHours(1));

        // When & Then
        assertThrows(BadRequestException.class, () ->
                eventService.createEvent(userId, newEventDto));
    }

    @Test
    void getEventPublic_WhenEventPublished_ShouldReturnEvent() {
        // Given
        Long eventId = 1L;
        User user = createTestUser(1L);
        Category category = createTestCategory(1L);
        Event event = createTestEvent(eventId, user, category, EventState.PUBLISHED);

        when(baseService.getEventById(eventId)).thenReturn(event);
        when(commentService.getPublishedCommentsCount(eventId)).thenReturn(5L);
        doNothing().when(statsTrackingService).updateEventViews(eventId);

        // When
        EventFullDto result = eventService.getEventPublic(eventId, request);

        // Then
        assertNotNull(result);
        assertEquals(eventId, result.getId());
        verify(statsTrackingService).updateEventViews(eventId);
    }

    @Test
    void getEventPublic_WhenEventNotPublished_ShouldThrowException() {
        // Given
        Long eventId = 1L;
        User user = createTestUser(1L);
        Category category = createTestCategory(1L);
        Event event = createTestEvent(eventId, user, category, EventState.PENDING);

        when(baseService.getEventById(eventId)).thenReturn(event);

        // When & Then
        assertThrows(NotFoundException.class, () ->
                eventService.getEventPublic(eventId, request));
    }

    @Test
    void updateEventByAdmin_WhenValidData_ShouldUpdateEvent() {
        // Given
        Long eventId = 1L;
        User user = createTestUser(1L);
        Category category = createTestCategory(1L);
        Event event = createTestEvent(eventId, user, category, EventState.PENDING);

        UpdateEventAdminRequest updateRequest = UpdateEventAdminRequest.builder()
                .annotation("Updated Annotation")
                .title("Updated Title")
                .stateAction(AdminStateAction.PUBLISH_EVENT)
                .build();

        when(baseService.getEventById(eventId)).thenReturn(event);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(commentService.getPublishedCommentsCount(eventId)).thenReturn(0L);

        // When
        EventFullDto result = eventService.updateEventByAdmin(eventId, updateRequest);

        // Then
        assertNotNull(result);
        verify(eventRepository).save(event);
    }

}