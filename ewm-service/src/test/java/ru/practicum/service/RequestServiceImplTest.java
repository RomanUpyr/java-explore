package ru.practicum.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.exception.*;
import ru.practicum.model.*;
import ru.practicum.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private BaseService baseService;

    @InjectMocks
    private RequestServiceImpl requestService;

    private User createTestUser(Long id) {
        return User.builder()
                .id(id)
                .name("User " + id)
                .email("user" + id + "@email.com")
                .build();
    }

    private Event createTestEvent(Long id, User initiator, EventState state) {
        return Event.builder()
                .id(id)
                .annotation("Event " + id)
                .category(Category.builder().id(1L).name("Category").build())
                .eventDate(LocalDateTime.now().plusDays(1))
                .initiator(initiator)
                .state(state)
                .participantLimit(10)
                .confirmedRequests(5)
                .requestModeration(true)
                .paid(false)
                .title("Event " + id)
                .build();
    }

    private ParticipationRequest createTestRequest(Long id, User requester, Event event, RequestStatus status) {
        return ParticipationRequest.builder()
                .id(id)
                .requester(requester)
                .event(event)
                .status(status)
                .created(LocalDateTime.now())
                .build();
    }

    @Test
    void createRequest_WithValidData_ShouldCreateRequest() {
        // Given
        Long userId = 2L;
        Long eventId = 1L;
        User requester = createTestUser(userId);
        User initiator = createTestUser(1L);
        Event event = createTestEvent(eventId, initiator, EventState.PUBLISHED);

        when(baseService.getUserById(userId)).thenReturn(requester);
        when(baseService.getEventById(eventId)).thenReturn(event);
        when(requestRepository.existsByEventIdAndRequesterId(eventId, userId)).thenReturn(false);
        when(requestRepository.save(any(ParticipationRequest.class))).thenAnswer(invocation -> {
            ParticipationRequest request = invocation.getArgument(0);
            request.setId(1L);
            return request;
        });

        // When
        ParticipationRequestDto result = requestService.createRequest(userId, eventId);

        // Then
        assertNotNull(result);
        assertEquals(RequestStatus.PENDING, result.getStatus());
        verify(requestRepository).save(any(ParticipationRequest.class));
    }

    @Test
    void createRequest_WhenUserIsInitiator_ShouldThrowException() {
        // Given
        Long userId = 1L;
        Long eventId = 1L;
        User user = createTestUser(userId);
        Event event = createTestEvent(eventId, user, EventState.PUBLISHED);

        when(baseService.getUserById(userId)).thenReturn(user);
        when(baseService.getEventById(eventId)).thenReturn(event);

        // When & Then
        assertThrows(ConflictException.class, () ->
                requestService.createRequest(userId, eventId));
    }

    @Test
    void createRequest_WhenEventNotPublished_ShouldThrowException() {
        // Given
        Long userId = 2L;
        Long eventId = 1L;
        User requester = createTestUser(userId);
        User initiator = createTestUser(1L);
        Event event = createTestEvent(eventId, initiator, EventState.PENDING);

        when(baseService.getUserById(userId)).thenReturn(requester);
        when(baseService.getEventById(eventId)).thenReturn(event);

        // When & Then
        assertThrows(ConflictException.class, () ->
                requestService.createRequest(userId, eventId));
    }

    @Test
    void createRequest_WhenParticipantLimitReached_ShouldThrowException() {
        // Given
        Long userId = 2L;
        Long eventId = 1L;
        User requester = createTestUser(userId);
        User initiator = createTestUser(1L);
        Event event = createTestEvent(eventId, initiator, EventState.PUBLISHED);
        event.setConfirmedRequests(10);
        event.setParticipantLimit(10);

        when(baseService.getUserById(userId)).thenReturn(requester);
        when(baseService.getEventById(eventId)).thenReturn(event);

        // When & Then
        assertThrows(ConflictException.class, () ->
                requestService.createRequest(userId, eventId));
    }

    @Test
    void cancelRequest_WithValidData_ShouldCancelRequest() {
        // Given
        Long userId = 1L;
        Long requestId = 1L;
        User user = createTestUser(userId);
        Event event = createTestEvent(1L, createTestUser(2L), EventState.PUBLISHED);
        ParticipationRequest request = createTestRequest(requestId, user, event, RequestStatus.PENDING);

        when(baseService.userExists(userId)).thenReturn(true);
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(request);

        // When
        ParticipationRequestDto result = requestService.cancelRequest(userId, requestId);

        // Then
        assertNotNull(result);
        assertEquals(RequestStatus.CANCELED, result.getStatus());
        verify(requestRepository).save(request);
    }
}