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

    @AfterReturning("publicEndpoints() && args(.., request)")
    public void saveHitAfterSuccessfulRequest(JoinPoint joinPoint, HttpServletRequest request) {
        try {
            String uri = request.getRequestURI();
            String clientIp = baseService.getClientIp(request);

            log.debug("Аспект сработал для URI: {}, IP: {}", uri, clientIp);

            statsTrackingService.trackHit(request);

            if (isEventDetailsRequest(uri)) {
                try {
                    Long eventId = extractEventIdFromUri(uri);
                    if (eventId != null) {
                        log.debug("Обновление просмотров для события ID: {}", eventId);
                        statsTrackingService.updateEventViewsAsync(eventId);
                    }
                } catch (Exception e) {
                    log.debug("Could not extract event ID from URI: {}", uri);
                }
            }

        } catch (Exception e) {
            log.error("Ошибка в аспекте при сохранении статистики", e);
        }
    }

    private boolean isEventDetailsRequest(String uri) {
        return uri.startsWith("/events/") && !uri.equals("/events");
    }

    Long extractEventIdFromUri(String uri) {
        try {
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
