package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Конфигурационный класс для настройки WebClient.
 */
@Slf4j
@Configuration
public class WebClientConfig {

    /**
     * URL сервиса статистики из конфигурации.
     */
    @Value("${stats.service.url:http://localhost:9090}")
    private String statsServiceUrl;

    /**
     * Создает и настраивает WebClient для HTTP запросов.
     */
    @Bean
    public WebClient webClient() {
        log.debug("Инициализация WebClient с URL: {}", statsServiceUrl);
        return WebClient.builder()
                .baseUrl(statsServiceUrl)
                .defaultHeader("User-Agent", "Stats-Client/1.0")
                .build();
    }

    /**
     * Создает и настраивает клиент для работы с сервисом статистики.
     */
    @Bean
    public StatsClient statsClient(WebClient webClient) {
        log.debug("Инициализация StatsClient с URL: {}", statsServiceUrl);
        return new StatsClient(webClient);
    }
}