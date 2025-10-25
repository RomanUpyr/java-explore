package ru.practicum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * DTO для возврата статистики по посещениям.
 */
@Getter
@AllArgsConstructor
@ToString
public class ViewStats {
    /**
     * Название сервиса.
     */
    private final String app;

    /**
     * URI сервиса, для которого собрана статистика.
     */
    private final String uri;

    /**
     * Количество просмотров для данной комбинации app и uri.
     */
    private final Long hits;
}
