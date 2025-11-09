package ru.practicum.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import ru.practicum.service.BaseService;
import ru.practicum.service.StatsTrackingService;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class StatsAspect {
    private final StatsTrackingService statsTrackingService;
    private final BaseService baseService;

    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping) && " +
            "within(ru.practicum.controller.PublicController)")
    public void publicEndpoints() {
    }

    // Дополнительный pointcut для методов без HttpServletRequest в параметрах
    @AfterReturning("publicEndpoints()")
    public void saveHitForMethodsWithoutRequest(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg instanceof HttpServletRequest) {
                    HttpServletRequest request = (HttpServletRequest) arg;
                    statsTrackingService.trackHit(request);
                    log.debug("Статистика сохранена через аспект для URI: {}", request.getRequestURI());
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Ошибка в аспекте при сохранении статистики", e);
        }
    }

    @AfterReturning("publicEndpoints() && args(.., request)")
    public void saveHitAfterSuccessfulRequest(JoinPoint joinPoint, HttpServletRequest request) {
        try {
            String uri = request.getRequestURI();
            String clientIp = baseService.getClientIp(request);

            // Отправляем статистику
            statsTrackingService.trackHit(request);

            // Обновляем просмотры если это запрос к событию
            if (uri.startsWith("/events/")) {
                try {
                    Long eventId = extractEventIdFromUri(uri);
                    if (eventId != null) {
                        statsTrackingService.updateEventViews(eventId, clientIp);
                    }
                } catch (Exception e) {
                    log.debug("Could not extract event ID from URI: {}", uri);
                }
            }

        } catch (Exception e) {
            log.error("Ошибка в аспекте при сохранении статистики", e);
        }
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            // URI вида "/events/123" или "/events/123/something"
            String[] parts = uri.split("/");
            for (int i = 0; i < parts.length; i++) {
                if ("events".equals(parts[i]) && i + 1 < parts.length) {
                    return Long.parseLong(parts[i + 1]);
                }
            }
        } catch (NumberFormatException e) {
            log.debug("Invalid event ID in URI: {}", uri);
        }
        return null;
    }
}
