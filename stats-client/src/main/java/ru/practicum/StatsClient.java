package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * HTTP-клиент для взаимодействия с сервисом статистики.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClient {
    /**
     * WebClient для выполнения HTTP запросов.
     */
    private final WebClient webClient;
    /**
     * Форматтер для преобразования даты-времени в строку.
     */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Сохраняет информацию о посещении эндпоинта в сервисе статистики.
     */
    public void saveHit(EndpointHitRequest hitRequest) {
        log.debug("Отправка POST запроса на сохранение статистики: {}", hitRequest);

        try {
            webClient.post()
                    .uri("/hit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(hitRequest)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> {
                                log.error("Ошибка при сохранении статистики. HTTP статус: {}",
                                        clientResponse.statusCode());
                                return Mono.error(new RuntimeException(
                                        "Ошибка сервиса статистики: " + clientResponse.statusCode()));
                            }
                    )
                    .toBodilessEntity()
                    .doOnSuccess(response ->
                            log.debug("Статистика успешно сохранена. HTTP статус: {}",
                                    response.getStatusCode()))
                    .doOnError(error ->
                            log.error("Ошибка при сохранении статистики: {}", error.getMessage()))
                    .block();

        } catch (Exception e) {
            log.error("Исключение при сохранении статистики: {}", e.getMessage());
        }
    }

    /**
     * Получает статистику по посещениям за указанный период.
     */
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                                    List<String> uris, Boolean unique) {

        String url = buildStatsUrl(start, end, uris, unique);
        log.debug("Выполнение GET запроса статистики: {}", url);

        try {
            List<ViewStats> stats = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stats")
                            .queryParam("start", encodeDateTime(start))
                            .queryParam("end", encodeDateTime(end))
                            .queryParam("unique", unique != null ? unique : false)
                            .queryParam("uris", uris != null && !uris.isEmpty() ? uris.toArray(new String[0]) : new String[0])
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> {
                                log.error("Ошибка при получении статистики. HTTP статус: {}",
                                        clientResponse.statusCode());
                                return Mono.error(new RuntimeException(
                                        "Ошибка сервиса статистики: " + clientResponse.statusCode()));
                            }
                    )
                    .bodyToMono(new ParameterizedTypeReference<List<ViewStats>>() {})
                    .doOnSuccess(response ->
                            log.debug("Успешно получено {} записей статистики", response.size()))
                    .doOnError(error ->
                            log.error("Ошибка при получении статистики: {}", error.getMessage()))
                    .onErrorReturn(List.of())
                    .block();

            log.info("Получено {} записей статистики", stats != null ? stats.size() : 0);
            return stats != null ? stats : List.of();

        } catch (Exception e) {
            log.error("Исключение при получении статистики: {}", e.getMessage());
            return List.of();
        }
    }


    /**
     * Строит URL для запроса статистики с параметрами.
     */
    private String buildStatsUrl(LocalDateTime start, LocalDateTime end,
                                 List<String> uris, Boolean unique) {

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromPath("/stats")
                .queryParam("start", encodeDateTime(start))
                .queryParam("end", encodeDateTime(end))
                .queryParam("unique", unique != null ? unique : false);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                uriBuilder.queryParam("uris", uri);
            }
        }

        return uriBuilder.toUriString();
    }

    /**
     * Преобразует LocalDateTime в строку заданного формата.
     */
    private String encodeDateTime(LocalDateTime dateTime) {
        return dateTime.format(FORMATTER);
    }

    /**
     * Проверяет доступность сервиса статистики.
     */
    public boolean isServiceAvailable() {
        try {
            return webClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> response.contains("\"status\":\"UP\""))
                    .onErrorReturn(false)
                    .blockOptional()
                    .orElse(false);
        } catch (Exception e) {
            log.warn("Сервис статистики недоступен: {}", e.getMessage());
            return false;
        }
    }
}
