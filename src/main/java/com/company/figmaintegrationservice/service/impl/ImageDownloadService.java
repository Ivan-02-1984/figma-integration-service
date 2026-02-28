package com.company.figmaintegrationservice.service.impl;

import com.company.figmaintegrationservice.service.IImageDownloadService;
import com.company.figmaintegrationservice.service.IRateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для загрузки изображений из URL.
 * Отвечает только за загрузку изображений (Single Responsibility Principle).
 * Использует кэширование, синхронизацию и rate limiting для предотвращения рейт-лимита Figma.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageDownloadService implements IImageDownloadService {

    private final IRateLimitService rateLimitService;

    @Value("${figma.archive.image-timeout-seconds:30}")
    private int imageTimeoutSeconds;

    @Value("${figma.archive.max-image-size-mb:50}")
    private int maxImageSizeMb;

    @Value("${figma.archive.min-delay-between-downloads-ms:300}")
    private int minDelayBetweenDownloadsMs;

    // Кэш для уже загруженных изображений
    private final Map<String, byte[]> imageCache = new ConcurrentHashMap<>();
    
    // Синхронизация загрузок одинаковых URL - только один поток загружает, остальные ждут
    private final Map<String, Object> urlLocks = new ConcurrentHashMap<>();

    /**
     * Скачивает изображение по URL с повторными попытками.
     * Использует кэширование и синхронизацию для предотвращения одновременных загрузок одинаковых URL.
     *
     * @param imageUrl URL изображения
     * @param maxRetries максимальное количество попыток
     * @return массив байтов изображения или null, если не удалось скачать
     */
    @Override
    public byte[] downloadImageWithRetry(String imageUrl, int maxRetries) throws IOException, InterruptedException {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IOException("URL изображения пустой");
        }

        // Проверяем кэш
        byte[] cached = imageCache.get(imageUrl);
        if (cached != null) {
            log.debug("✅ Изображение найдено в кэше: {}", imageUrl);
            return cached;
        }

        // Получаем блокировку для этого URL (только один поток загружает одинаковый URL)
        Object lock = urlLocks.computeIfAbsent(imageUrl, k -> new Object());
        
        try {
            synchronized (lock) {
                // Двойная проверка кэша после получения блокировки
                cached = imageCache.get(imageUrl);
                if (cached != null) {
                    log.debug("✅ Изображение найдено в кэше (после блокировки): {}", imageUrl);
                    return cached;
                }

                // Получаем разрешение от rate limiter перед загрузкой
                rateLimitService.acquire();
                
                // Дополнительная задержка для гарантированного соблюдения лимитов
                if (minDelayBetweenDownloadsMs > 0) {
                    Thread.sleep(minDelayBetweenDownloadsMs);
                }
                
                // Загружаем изображение
                byte[] result = downloadImageWithRetryInternal(imageUrl, maxRetries);
                
                if (result != null && result.length > 0) {
                    // Сохраняем в кэш только успешно загруженные изображения
                    imageCache.put(imageUrl, result);
                    log.debug("✅ Изображение загружено и сохранено в кэш: {}", imageUrl);
                }
                
                return result;
            }
        } finally {
            // Удаляем блокировку после завершения (но оставляем в кэше)
            urlLocks.remove(imageUrl);
        }
    }

    /**
     * Внутренний метод для загрузки изображения с повторными попытками.
     */
    private byte[] downloadImageWithRetryInternal(String imageUrl, int maxRetries) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                byte[] data = downloadImageToBytes(imageUrl);
                if (data != null && data.length > 0) {
                    if (attempt > 0) {
                        log.info("✅ Удалось скачать после {} попыток: {}", attempt, imageUrl);
                    }
                    return data;
                } else {
                    throw new IOException("Получены пустые данные");
                }
            } catch (IOException e) {
                lastException = e;
                attempt++;

                if (attempt < maxRetries) {
                    long waitTime = (long) Math.pow(2, attempt) * 1000;
                    log.warn("⚠️ Попытка {} не удалась для {}, повтор через {} сек (ошибка: {})",
                            attempt, imageUrl, waitTime / 1000, e.getMessage());
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Поток прерван", ie);
                    }
                }
            }
        }

        log.error("❌ Не удалось скачать {} после {} попыток. Последняя ошибка: {}",
                imageUrl, maxRetries, lastException != null ? lastException.getMessage() : "null");
        return null;
    }

    /**
     * Скачивает изображение в массив байтов.
     */
    private byte[] downloadImageToBytes(String imageUrl) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(imageUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(imageTimeoutSeconds * 1000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8");
            conn.setRequestProperty("Connection", "keep-alive");

            int responseCode = conn.getResponseCode();

            if (responseCode == 429) {
                String retryAfter = conn.getHeaderField("Retry-After");
                throw new IOException("Rate limit exceeded. Retry after: " + retryAfter);
            }

            if (responseCode == 403) {
                throw new IOException("Доступ запрещен (403) для URL: " + imageUrl);
            }

            if (responseCode == 404) {
                throw new IOException("Изображение не найдено (404) для URL: " + imageUrl);
            }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream in = conn.getInputStream()) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    int totalBytes = 0;
                    int maxBytes = maxImageSizeMb * 1024 * 1024;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                        if (totalBytes > maxBytes) {
                            throw new IOException("Изображение слишком большое (>" + maxImageSizeMb + " МБ)");
                        }
                    }

                    byte[] result = out.toByteArray();

                    if (result == null || result.length == 0) {
                        throw new IOException("Скачан пустой файл");
                    }

                    log.debug("✅ Скачано {} байт с {}", result.length, imageUrl);
                    return result;
                }
            } else {
                throw new IOException("HTTP " + responseCode + " for URL: " + imageUrl);
            }
        } catch (Exception e) {
            log.error("❌ Ошибка скачивания {}: {}", imageUrl, e.getMessage());
            throw new IOException("Не удалось скачать: " + imageUrl, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
