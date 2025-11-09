package ru.practicum.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import ru.practicum.service.StatsTrackingService;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class StatsAspect {
    private final StatsTrackingService statsTrackingService;

    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping) && " +
            "within(ru.practicum.controller.PublicController)")
    public void publicEndpoints() {
    }

    @AfterReturning("publicEndpoints() && args(.., request)")
    public void saveHitAfterSuccessfulRequest(JoinPoint joinPoint, HttpServletRequest request) {
        try {
            statsTrackingService.trackHit(request);
            log.debug("Статистика сохранена через аспект для URI: {}", request.getRequestURI());

        } catch (Exception e) {
            log.error("Ошибка в аспекте при сохранении статистики", e);
        }
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
}
