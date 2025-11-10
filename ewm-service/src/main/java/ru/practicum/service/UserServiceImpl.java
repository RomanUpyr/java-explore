package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.UserDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;
import ru.practicum.exception.ConflictException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Реализация сервиса для работы с пользователями
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final BaseService baseService;
    private final CommentService commentService;

    /**
     * Получение всех пользователей
     */
    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        log.debug("Getting users: ids={}, from={}, size={}", ids, from, size);

        List<User> users;
        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(baseService.createPageRequest(from, size)).getContent();
        } else {
            users = userRepository.findAllById(ids);
        }

        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Создание пользователя
     */
    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        log.debug("Creating user: {}", userDto);

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ConflictException("User with email=" + userDto.getEmail() + " already exists");
        }

        User user = User.builder()
                .name(userDto.getName())
                .email(userDto.getEmail())
                .build();

        User savedUser = userRepository.save(user);
        log.debug("User created with id={}", savedUser.getId());

        return convertToDto(savedUser);
    }

    /**
     * Удаление пользователя
     */
    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.debug("Deleting user id={}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        commentService.deleteUserComments(userId);
        userRepository.deleteById(userId);
        log.debug("User id={} deleted", userId);
    }

    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
