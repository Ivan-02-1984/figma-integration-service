package com.company.figmaintegrationservice.service;

import com.company.figmaintegrationservice.client.dto.FigmaFileResponse;
import reactor.core.publisher.Mono;

import java.util.concurrent.Callable;

/**
 * Интерфейс для сервиса кэширования файлов Figma.
 */
public interface IFigmaFileCacheService {
    /**
     * Получает значение из кэша или вычисляет его, если отсутствует.
     *
     * @param key ключ кэша
     * @param supplier функция для вычисления значения, если его нет в кэше
     * @return Mono с результатом из кэша или вычисленным значением
     */
    Mono<FigmaFileResponse> getOrCompute(String key, Callable<Mono<FigmaFileResponse>> supplier);

    /**
     * Очищает кэш для указанного ключа.
     */
    void evict(String key);

    /**
     * Очищает весь кэш.
     */
    void evictAll();
}
