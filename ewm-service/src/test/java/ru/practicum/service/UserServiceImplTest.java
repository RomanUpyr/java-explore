package ru.practicum.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.UserDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BaseService baseService;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private UserServiceImpl userService;

    private User createTestUser(Long id, String name, String email) {
        return User.builder()
                .id(id)
                .name(name)
                .email(email)
                .build();
    }

    @Test
    void getUsers_WithIds_ShouldReturnFilteredUsers() {
        // Given
        User user1 = createTestUser(1L, "User 1", "user1@email.com");
        User user2 = createTestUser(2L, "User 2", "user2@email.com");
        List<Long> ids = List.of(1L, 2L);

        when(userRepository.findAllById(ids)).thenReturn(List.of(user1, user2));

        // When
        List<UserDto> result = userService.getUsers(ids, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findAllById(ids);
    }

    @Test
    void createUser_WithUniqueEmail_ShouldCreateUser() {
        // Given
        UserDto userDto = UserDto.builder()
                .name("New User")
                .email("new@email.com")
                .build();
        User savedUser = createTestUser(1L, "New User", "new@email.com");

        when(userRepository.existsByEmail("new@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        UserDto result = userService.createUser(userDto);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("New User", result.getName());
        assertEquals("new@email.com", result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WithDuplicateEmail_ShouldThrowException() {
        // Given
        UserDto userDto = UserDto.builder()
                .name("New User")
                .email("duplicate@email.com")
                .build();

        when(userRepository.existsByEmail("duplicate@email.com")).thenReturn(true);

        // When & Then
        assertThrows(ConflictException.class, () ->
                userService.createUser(userDto));
    }

    @Test
    void deleteUser_WhenUserExists_ShouldDeleteUser() {
        // Given
        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);
        doNothing().when(commentService).deleteUserComments(userId);
        doNothing().when(userRepository).deleteById(userId);

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).deleteById(userId);
        verify(commentService).deleteUserComments(userId);
    }

    @Test
    void deleteUser_WhenUserNotExists_ShouldThrowException() {
        // Given
        Long userId = 999L;

        when(userRepository.existsById(userId)).thenReturn(false);

        // When & Then
        assertThrows(NotFoundException.class, () ->
                userService.deleteUser(userId));
    }

    @Test
    void createUser_WithInvalidEmail_ShouldUseValidation() {
        // Given
        UserDto userDto = UserDto.builder()
                .name("Valid User")
                .email("invalid-email")
                .build();

        // When & Then
        assertDoesNotThrow(() -> {
            when(userRepository.existsByEmail("invalid-email")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(createTestUser(1L, "Valid User", "invalid-email"));
            userService.createUser(userDto);
        });
    }
}