package ru.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.practicum.EndpointHitRequest;
import ru.practicum.ViewStats;
import ru.practicum.exception.BadRequestException;
import ru.practicum.model.EndpointHit;
import ru.practicum.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb"
})
class StatsControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StatsService statsService;

    private EndpointHitRequest validHitRequest;
    private EndpointHit savedEndpointHit;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        startTime = LocalDateTime.now().minusDays(1);
        endTime = LocalDateTime.now();

        validHitRequest = EndpointHitRequest.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(startTime.toString())
                .build();

        savedEndpointHit = EndpointHit.builder()
                .id(1L)
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(startTime)
                .build();
    }

    @Test
    void hit_WhenValidRequest_ShouldReturnCreated() throws Exception {
        Mockito.when(statsService.saveHit(any(EndpointHit.class)))
                .thenReturn(savedEndpointHit);

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validHitRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.app").value("ewm-main-service"))
                .andExpect(jsonPath("$.uri").value("/events/1"))
                .andExpect(jsonPath("$.ip").value("192.168.1.1"));
    }

    @Test
    void hit_WhenInvalidRequest_ShouldReturnBadRequest() throws Exception {
        EndpointHitRequest invalidRequest = EndpointHitRequest.builder()
                .app("")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(startTime.toString())
                .build();

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStats_WhenValidRequest_ShouldReturnStats() throws Exception {
        List<ViewStats> expectedStats = List.of(
                new ViewStats("ewm-main-service", "/events/1", 10L)
        );

        Mockito.when(statsService.getStats(
                        Mockito.any(LocalDateTime.class),
                        Mockito.any(LocalDateTime.class),
                        Mockito.isNull(),
                        Mockito.eq(false)))
                .thenReturn(expectedStats);

        mockMvc.perform(get("/stats")
                        .param("start", startTime.toString())
                        .param("end", endTime.toString())
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].app").value("ewm-main-service"))
                .andExpect(jsonPath("$[0].uri").value("/events/1"))
                .andExpect(jsonPath("$[0].hits").value(10));
    }

    @Test
    void getStats_WithUris_ShouldReturnFilteredStats() throws Exception {
        List<ViewStats> expectedStats = List.of(
                new ViewStats("ewm-main-service", "/events/1", 5L),
                new ViewStats("ewm-main-service", "/events/2", 3L)
        );

        Mockito.when(statsService.getStats(any(LocalDateTime.class), any(LocalDateTime.class),
                        anyList(), anyBoolean()))
                .thenReturn(expectedStats);

        mockMvc.perform(get("/stats")
                        .param("start", startTime.toString())
                        .param("end", endTime.toString())
                        .param("uris", "/events/1", "/events/2")
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getStats_WhenStartAfterEnd_ShouldReturnBadRequest() throws Exception {
        LocalDateTime invalidStart = endTime.plusDays(1);

        Mockito.when(statsService.getStats(any(LocalDateTime.class), any(LocalDateTime.class),
                        anyList(), anyBoolean()))
                .thenThrow(new BadRequestException("Начальная дата не может быть позже конечной"));

        mockMvc.perform(get("/stats")
                        .param("start", invalidStart.toString())
                        .param("end", endTime.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStats_WithUniqueTrue_ShouldReturnUniqueStats() throws Exception {
        List<ViewStats> expectedStats = List.of(
                new ViewStats("ewm-main-service", "/events/1", 3L)
        );

        Mockito.when(statsService.getStats(any(LocalDateTime.class), any(LocalDateTime.class),
                        Mockito.isNull(), Mockito.eq(true)))
                .thenReturn(expectedStats);

        mockMvc.perform(get("/stats")
                        .param("start", startTime.toString())
                        .param("end", endTime.toString())
                        .param("unique", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("ewm-main-service"))
                .andExpect(jsonPath("$[0].uri").value("/events/1"))
                .andExpect(jsonPath("$[0].hits").value(3));
    }

    @Test
    void getStats_WithEncodedUris_ShouldDecodeProperly() throws Exception {
        String encodedUri = "/events%2F1";
        List<ViewStats> expectedStats = List.of(
                new ViewStats("ewm-main-service", "/events/1", 5L)
        );

        Mockito.when(statsService.getStats(any(LocalDateTime.class), any(LocalDateTime.class),
                        anyList(), anyBoolean()))
                .thenReturn(expectedStats);

        mockMvc.perform(get("/stats")
                        .param("start", startTime.toString())
                        .param("end", endTime.toString())
                        .param("uris", encodedUri)
                        .param("unique", "false"))
                .andExpect(status().isOk());
    }
}