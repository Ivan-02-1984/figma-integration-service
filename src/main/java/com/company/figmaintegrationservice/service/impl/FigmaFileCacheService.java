package com.company.figmaintegrationservice.service.impl;

import com.company.figmaintegrationservice.client.dto.FigmaFileResponse;
import com.company.figmaintegrationservice.service.IFigmaFileCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.Callable;

/**
 * Сервис для кэширования реактивных Mono с использованием Spring Cache.
 * Обеспечивает правильную работу кэша с реактивными типами.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FigmaFileCacheService implements IFigmaFileCacheService {

    private static final String CACHE_NAME = "figmaFiles";
    private final CacheManager cacheManager;

    /**
     * Получает значение из кэша или вычисляет его, если отсутствует.
     * 
     * @param key ключ кэша
     * @param supplier функция для вычисления значения, если его нет в кэше
     * @return Mono с результатом из кэша или вычисленным значением
     */
    @Override
    public Mono<FigmaFileResponse> getOrCompute(String key, Callable<Mono<FigmaFileResponse>> supplier) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            log.warn("Кэш {} не найден, пропускаем кэширование", CACHE_NAME);
            try {
                return supplier.call();
            } catch (Exception e) {
                return Mono.error(e);
            }
        }

        Cache.ValueWrapper wrapper = cache.get(key);
        if (wrapper != null && wrapper.get() != null) {
            log.debug("✅ Найдено в кэше: {}", key);
            return Mono.just((FigmaFileResponse) wrapper.get());
        }

        log.debug("📤 Значение отсутствует в кэше, вычисляем: {}", key);
        try {
            return supplier.call()
                    .doOnNext(value -> {
                        if (value != null) {
                            cache.put(key, value);
                            log.debug("💾 Сохранено в кэш: {}", key);
                        }
                    });
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    /**
     * Очищает кэш для указанного ключа.
     */
    @Override
    public void evict(String key) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.evict(key);
            log.debug("🗑️ Удалено из кэша: {}", key);
        }
    }

    /**
     * Очищает весь кэш.
     */
    @Override
    public void evictAll() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.clear();
            log.debug("🗑️ Кэш полностью очищен");
        }
    }
}
