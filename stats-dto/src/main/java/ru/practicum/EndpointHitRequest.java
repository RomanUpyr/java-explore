package ru.practicum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;


/**
 * DTO для запроса на сохранение информации о посещении эндпоинта.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitRequest {
    /**
     * Уникальный идентификатор записи.
     */
    private Long id;

    /**
     * Идентификатор сервиса, для которого записывается информация.
     */
    @NotBlank(message = "App cannot be blank")
    private String app;

    /**
     * URI ресурса, к которому был осуществлен запрос.
     */
    @NotBlank(message = "URI cannot be blank")
    private String uri;

    /**
     * IP-адрес пользователя, осуществившего запрос.
     */
    @NotBlank(message = "IP cannot be blank")
    private String ip;

    /**
     * Дата и время, когда был совершен запрос к эндпоинту.
     */
    @NotBlank
    private String timestamp;

}
