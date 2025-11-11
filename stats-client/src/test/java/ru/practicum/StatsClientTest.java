package ru.practicum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private StatsClient statsClient;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        statsClient = new StatsClient(webClient);
        startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        endTime = LocalDateTime.of(2024, 1, 1, 12, 0);
    }

    @Test
    void saveHit_WhenSuccess_ShouldLogSuccess() {

        EndpointHitRequest hitRequest = new EndpointHitRequest();
        hitRequest.setApp("test-app");
        hitRequest.setUri("/test");
        hitRequest.setIp("192.168.1.1");
        LocalDateTime now = LocalDateTime.now();
        String timestampString = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        hitRequest.setTimestamp(timestampString);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/hit")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());


        statsClient.saveHit(hitRequest);

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/hit");
        verify(requestBodySpec).contentType(any());
        verify(requestBodySpec).bodyValue(hitRequest);
    }

    void saveHit_WhenError_ShouldLogError() {
        // Given
        EndpointHitRequest hitRequest = new EndpointHitRequest();

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/hit")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenAnswer(invocation -> {
            java.util.function.Predicate<HttpStatus> statusPredicate = invocation.getArgument(0);
            java.util.function.Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction = invocation.getArgument(1);

            if (statusPredicate.test(HttpStatus.BAD_REQUEST)) {
                ClientResponse clientResponse = ClientResponse.create(HttpStatus.BAD_REQUEST)
                        .header("Content-Type", "application/json")
                        .body("{\"error\": \"Bad Request\"}")
                        .build();
                exceptionFunction.apply(clientResponse);
            }
            return responseSpec;
        });

        when(responseSpec.toBodilessEntity()).thenReturn(Mono.error(new RuntimeException("Server error")));

        // When
        statsClient.saveHit(hitRequest);

        // Then
        verify(webClient).post();
    }

    @Test
    void getStats_WhenSuccess_ShouldReturnStats() {
        // Given
        List<ViewStats> expectedStats = List.of(
                new ViewStats("app1", "/events/1", 10L),
                new ViewStats("app2", "/events/2", 5L)
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);

        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);

        when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(expectedStats));

        // When
        List<ViewStats> result = statsClient.getStats(startTime, endTime, List.of("/events/1", "/events/2"), false);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("app1", result.get(0).getApp());
        assertEquals("/events/1", result.get(0).getUri());
        assertEquals(10L, result.get(0).getHits());
    }

    @Test
    void getStats_WhenEmptyUris_ShouldReturnStats() {
        // Given
        List<ViewStats> expectedStats = List.of();

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        // Используем any(ParameterizedTypeReference.class) вместо any(Class.class)
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(expectedStats));

        // When
        List<ViewStats> result = statsClient.getStats(startTime, endTime, null, true);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getStats_WhenServerError_ShouldReturnEmptyList() {

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenAnswer(invocation -> {
            var predicate = invocation.getArgument(0);
            var function = invocation.getArgument(1);
            if (predicate.test(HttpStatus.INTERNAL_SERVER_ERROR)) {
                function.apply(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());
            }
            return responseSpec;
        });
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.error(new RuntimeException("Server error")));

        List<ViewStats> result = statsClient.getStats(startTime, endTime, null, false);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getStats_WhenNullUnique_ShouldUseDefaultFalse() {
        List<ViewStats> expectedStats = List.of();

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(expectedStats));

        List<ViewStats> result = statsClient.getStats(startTime, endTime, null, null);

        assertNotNull(result);
        verify(requestHeadersUriSpec).uri(any());
    }

    @Test
    void isServiceAvailable_WhenServiceUp_ShouldReturnTrue() {
        // Given
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/actuator/health")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{\"status\":\"UP\"}"));

        boolean result = statsClient.isServiceAvailable();

        assertTrue(result);
    }

    @Test
    void isServiceAvailable_WhenServiceDown_ShouldReturnFalse() {

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/actuator/health")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new RuntimeException("Connection failed")));

        boolean result = statsClient.isServiceAvailable();

        assertFalse(result);
    }

    @Test
    void isServiceAvailable_WhenServiceStatusNotUp_ShouldReturnFalse() {

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/actuator/health")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{\"status\":\"DOWN\"}"));

        boolean result = statsClient.isServiceAvailable();

        assertFalse(result);
    }
}