package ru.practicum;

import ru.practicum.EndpointHitRequest;
import ru.practicum.ViewStats;
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
     * Базовый URL сервиса статистики.
     */
    private final String statsServiceUrl;
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
            // Строим и выполняем POST запрос с WebClient
            webClient.post()
                    .uri(statsServiceUrl + "/hit")
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
                    .uri(url)
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

            log.info("Получено {} записей статистики", stats.size());
            return stats;

        } catch (Exception e) {
            log.error("Исключение при получении статистики: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Асинхронная версия метода получения статистики.
     */
    public Mono<List<ViewStats>> getStatsAsync(LocalDateTime start, LocalDateTime end,
                                               List<String> uris, Boolean unique) {

        String url = buildStatsUrl(start, end, uris, unique);
        log.debug("Асинхронный запрос статистики: {}", url);

        return webClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> {
                            log.warn("Ошибка HTTP {} при асинхронном запросе статистики",
                                    clientResponse.statusCode());
                            return Mono.error(new RuntimeException(
                                    "Ошибка сервиса статистики: " + clientResponse.statusCode()));
                        }
                )
                .bodyToMono(new ParameterizedTypeReference<List<ViewStats>>() {})
                .doOnNext(stats ->
                        log.debug("Асинхронно получено {} записей", stats.size()))
                .doOnError(error ->
                        log.error("Ошибка в асинхронном запросе статистики: {}", error.getMessage()))
                .onErrorReturn(List.of());
    }

    /**
     * Строит URL для запроса статистики с параметрами.
     */
    private String buildStatsUrl(LocalDateTime start, LocalDateTime end,
                                 List<String> uris, Boolean unique) {

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(statsServiceUrl + "/stats")
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
                    .uri(statsServiceUrl + "/actuator/health")
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
