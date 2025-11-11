package ru.practicum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class StatsClientRealServiceIntegrationTest {

    private static final String STATS_SERVICE_IMAGE = "ru.practicum/stats-service:latest";
    private static final int STATS_SERVICE_PORT = 9090;

    @Container
    private static final GenericContainer<?> statsServiceContainer =
            new GenericContainer<>(DockerImageName.parse(STATS_SERVICE_IMAGE))
                    .withExposedPorts(STATS_SERVICE_PORT)
                    .waitingFor(Wait.forHttp("/actuator/health")
                            .forStatusCode(200)
                            .forPort(STATS_SERVICE_PORT));

    @Autowired
    private StatsClient statsClient;

    @LocalServerPort
    private Integer port;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String statsServiceUrl = String.format("http://%s:%d",
                statsServiceContainer.getHost(),
                statsServiceContainer.getMappedPort(STATS_SERVICE_PORT));

        registry.add("stats.service.url", () -> statsServiceUrl);
    }

    @BeforeEach
    void setUp() {
        // Ждем пока контейнер полностью запустится
        assertTrue(statsServiceContainer.isRunning());
    }

    @Test
    void integrationTest_WithRealStatsService() {
        // Given
        EndpointHitRequest hitRequest = new EndpointHitRequest();
        hitRequest.setApp("integration-test-app");
        hitRequest.setUri("/integration-test");
        hitRequest.setIp("10.0.0.1");
        LocalDateTime now = LocalDateTime.now();
        String timestampString = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        hitRequest.setTimestamp(timestampString);

        // When - сохраняем hit
        statsClient.saveHit(hitRequest);

        // Then - проверяем что сервис доступен
        assertTrue(statsClient.isServiceAvailable());

        // And - получаем статистику
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        List<ViewStats> stats = statsClient.getStats(start, end, List.of("/integration-test"), false);

        assertNotNull(stats);
    }

    @Test
    void serviceHealthCheck_ShouldReturnTrueWhenServiceIsRunning() {
        // When
        boolean isAvailable = statsClient.isServiceAvailable();

        // Then
        assertTrue(isAvailable, "Stats service should be available when container is running");
    }
}