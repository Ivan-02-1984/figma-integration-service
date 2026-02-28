package com.company.figmaintegrationservice.service.impl;

import com.company.figmaintegrationservice.service.IRateLimitService;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Сервис для ограничения скорости запросов к Figma API.
 * Использует Guava RateLimiter для гарантированного соблюдения лимитов.
 * 
 * Настройки по умолчанию: 3 запроса в секунду = ~180 запросов в минуту
 * (ниже лимита Figma ~200 запросов/минуту)
 */
@Slf4j
@Service
public class RateLimitService implements IRateLimitService, InitializingBean {

    @Value("${figma.rate-limit.requests-per-second:3.0}")
    private double requestsPerSecond;

    private RateLimiter rateLimiter;

    @Override
    public void afterPropertiesSet() {
        rateLimiter = RateLimiter.create(requestsPerSecond);
        log.info("🚦 Rate limiter инициализирован: {} запросов/сек ({} запросов/мин)",
                requestsPerSecond, (int)(requestsPerSecond * 60));
    }

    @Override
    public void acquire() throws InterruptedException {
        try {
            rateLimiter.acquire();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw e;
            }
            log.error("❌ Ошибка при получении разрешения rate limiter: {}", e.getMessage());
            throw new RuntimeException("Ошибка rate limiter", e);
        }
    }

    @Override
    public boolean tryAcquire(long timeoutMs) throws InterruptedException {
        try {
            return rateLimiter.tryAcquire(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw e;
            }
            log.error("❌ Ошибка при попытке получить разрешение rate limiter: {}", e.getMessage());
            return false;
        }
    }
}
