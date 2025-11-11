package ru.practicum;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class AbstractTestcontainersTest {

    private static final int STATS_SERVICE_PORT = 9090;

    @Container
    protected static final GenericContainer<?> statsServiceContainer =
            new GenericContainer<>(DockerImageName.parse("your-stats-service-image:latest"))
                    .withExposedPorts(STATS_SERVICE_PORT)
                    .waitingFor(Wait.forHttp("/actuator/health")
                            .forStatusCode(200)
                            .forPort(STATS_SERVICE_PORT));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String statsServiceUrl = String.format("http://%s:%s",
                statsServiceContainer.getHost(),
                statsServiceContainer.getMappedPort(STATS_SERVICE_PORT));

        registry.add("stats.service.url", () -> statsServiceUrl);
    }
}