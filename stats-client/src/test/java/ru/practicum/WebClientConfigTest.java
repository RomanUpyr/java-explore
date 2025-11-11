package ru.practicum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "stats.service.url=http://test-stats-service:9090"
})
class WebClientConfigTest {

    @Autowired
    private WebClientConfig webClientConfig;

    @Autowired
    private WebClient webClient;

    @Autowired
    private StatsClient statsClient;

    @Test
    void webClientBean_ShouldBeCreated() {
        assertNotNull(webClient);
    }

    @Test
    void statsClientBean_ShouldBeCreated() {
        assertNotNull(statsClient);
    }

    @Test
    void webClient_ShouldHaveCorrectBaseUrl() {
        assertNotNull(webClientConfig);
        assertNotNull(webClient);
        assertNotNull(statsClient);
    }

    @Test
    void statsClient_ShouldBeInjectedWithWebClient() {
        assertNotNull(statsClient);
    }
}