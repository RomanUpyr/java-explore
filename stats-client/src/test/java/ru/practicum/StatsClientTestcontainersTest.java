package ru.practicum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@Testcontainers
class StatsClientTestcontainersTest {

    @Container
    public static MockServerContainer mockServer = new MockServerContainer(
            DockerImageName.parse("mockserver/mockserver:5.15.0")
    );

    @Autowired
    private StatsClient statsClient;

    @Autowired
    private ObjectMapper objectMapper;

    private MockServerClient mockServerClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("stats.service.url", mockServer::getEndpoint);
    }

    @BeforeEach
    void setUp() {
        mockServerClient = new MockServerClient(
                mockServer.getHost(),
                mockServer.getServerPort()
        );
        mockServerClient.reset();
    }

    @Test
    void saveHit_WhenValidRequest_ShouldSendPostRequest() throws JsonProcessingException {
        // Given
        EndpointHitRequest hitRequest = createTestHitRequest();

        // Mock the response
        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/hit")
                        .withContentType(MediaType.APPLICATION_JSON)
                )
                .respond(response()
                        .withStatusCode(201)
                );

        // When
        statsClient.saveHit(hitRequest);

        // Then - verify the request was made
        mockServerClient.verify(
                request()
                        .withMethod("POST")
                        .withPath("/hit")
                        .withBody(objectMapper.writeValueAsString(hitRequest))
        );
    }

    @Test
    void getStats_WhenValidRequest_ShouldReturnStats() throws JsonProcessingException {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<String> uris = List.of("/events/1", "/events/2");

        List<ViewStats> expectedStats = List.of(
                new ViewStats("ewm-main-service", "/events/1", 15L),
                new ViewStats("ewm-main-service", "/events/2", 8L)
        );

        // Mock the response
        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/stats")
                        .withQueryStringParameter("start", start.format(StatsClient.FORMATTER))
                        .withQueryStringParameter("end", end.format(StatsClient.FORMATTER))
                        .withQueryStringParameter("uris", "/events/1", "/events/2")
                        .withQueryStringParameter("unique", "false")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(objectMapper.writeValueAsString(expectedStats))
                );

        // When
        List<ViewStats> result = statsClient.getStats(start, end, uris, false);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("ewm-main-service", result.get(0).getApp());
        assertEquals("/events/1", result.get(0).getUri());
        assertEquals(15L, result.get(0).getHits());
    }

    @Test
    void getStats_WhenServerReturnsError_ShouldReturnEmptyList() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        // Mock error response
        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/stats")
                )
                .respond(response()
                        .withStatusCode(500)
                );

        // When
        List<ViewStats> result = statsClient.getStats(start, end, null, false);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getStats_WhenUniqueTrue_ShouldPassCorrectParameter() throws JsonProcessingException {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        List<ViewStats> expectedStats = List.of(
                new ViewStats("ewm-main-service", "/events/1", 5L)
        );

        // Mock the response for unique=true
        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/stats")
                        .withQueryStringParameter("unique", "true")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(objectMapper.writeValueAsString(expectedStats))
                );

        // When
        List<ViewStats> result = statsClient.getStats(start, end, null, true);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getHits());
    }

    @Test
    void isServiceAvailable_WhenServiceIsUp_ShouldReturnTrue() {
        // Given
        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/actuator/health")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withBody("{\"status\":\"UP\"}")
                );

        // When
        boolean result = statsClient.isServiceAvailable();

        // Then
        assertTrue(result);
    }

    @Test
    void isServiceAvailable_WhenServiceIsDown_ShouldReturnFalse() {
        // Given
        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/actuator/health")
                )
                .respond(response()
                        .withStatusCode(503)
                        .withBody("{\"status\":\"DOWN\"}")
                );

        // When
        boolean result = statsClient.isServiceAvailable();

        // Then
        assertFalse(result);
    }

    @Test
    void isServiceAvailable_WhenServiceUnreachable_ShouldReturnFalse() {
        // Given - no mock setup, so connection will fail to the mock server

        // When
        boolean result = statsClient.isServiceAvailable();

        // Then
        assertFalse(result);
    }

    private EndpointHitRequest createTestHitRequest() {
        EndpointHitRequest hitRequest = new EndpointHitRequest();
        hitRequest.setApp("test-app");
        hitRequest.setUri("/test-endpoint");
        hitRequest.setIp("192.168.1.100");
        hitRequest.setTimestamp(LocalDateTime.now());
        return hitRequest;
    }
}