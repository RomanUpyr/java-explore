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
 * Сервис для работы с пользователями
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService extends BaseService {
    /**
     * Получение всех пользователей
     */
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        log.info("Getting users: ids={}, from={}, size={}", ids, from, size);

        List<User> users;
        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(createPageRequest(from, size)).getContent();
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
    @Transactional
    public UserDto createUser(UserDto userDto) {
        log.info("Creating user: {}", userDto);

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ConflictException("User with email=" + userDto.getEmail() + " already exists");
        }

        User user = User.builder()
                .name(userDto.getName())
                .email(userDto.getEmail())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created with id={}", savedUser.getId());

        return convertToDto(savedUser);
    }

    /**
     * Удаление пользователя
     */
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user id={}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        userRepository.deleteById(userId);
        log.info("User id={} deleted", userId);
    }

    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

}
