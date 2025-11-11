package ru.practicum.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.*;
import ru.practicum.model.Location;
import ru.practicum.service.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class EventServiceIntegrationTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Test
    void createAndGetEvent_IntegrationTest() {
        // Create user
        UserDto userDto = UserDto.builder()
                .name("Test User")
                .email("test@email.com")
                .build();
        UserDto createdUser = userService.createUser(userDto);

        // Create category
        NewCategoryDto categoryDto = NewCategoryDto.builder()
                .name("Test Category")
                .build();
        CategoryDto createdCategory = categoryService.createCategory(categoryDto);

        // Create event
        NewEventDto eventDto = NewEventDto.builder()
                .annotation("Test Annotation")
                .description("Test Description")
                .title("Test Event")
                .category(createdCategory.getId())
                .eventDate(LocalDateTime.now().plusHours(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .location(new Location(55.7558F, 37.6173F))
                .build();

        EventFullDto createdEvent = eventService.createEvent(createdUser.getId(), eventDto);

        // Verify
        assertNotNull(createdEvent.getId());
        assertEquals("Test Event", createdEvent.getTitle());
        assertEquals(createdUser.getId(), createdEvent.getInitiator().getId());
    }

    @Test
    void getEventsByUser_IntegrationTest() {
        // Setup
        UserDto user = userService.createUser(UserDto.builder()
                .name("User1")
                .email("user1@email.com")
                .build());

        // Test
        List<EventShortDto> events = eventService.getEventsByUser(user.getId(), 0, 10);

        // Verify
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }
}