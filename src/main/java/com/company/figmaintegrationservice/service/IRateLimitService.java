package com.company.figmaintegrationservice.service;

/**
 * Интерфейс для сервиса ограничения скорости запросов (rate limiting).
 */
public interface IRateLimitService {
    /**
     * Получает разрешение на выполнение запроса.
     * Блокирует поток до получения разрешения в соответствии с лимитом.
     *
     * @throws InterruptedException если поток был прерван во время ожидания
     */
    void acquire() throws InterruptedException;

    /**
     * Получает разрешение на выполнение запроса с таймаутом.
     *
     * @param timeoutMs таймаут в миллисекундах
     * @return true если разрешение получено, false если истек таймаут
     * @throws InterruptedException если поток был прерван во время ожидания
     */
    boolean tryAcquire(long timeoutMs) throws InterruptedException;
}
