package com.company.figmaintegrationservice.service;

import java.util.concurrent.Callable;

/**
 * Интерфейс для сервиса метрик.
 */
public interface IMetricsService {
    /**
     * Измеряет время выполнения операции создания архива.
     */
    <T> T measureArchiveCreation(Callable<T> action) throws Exception;

    /**
     * Увеличивает счетчик скачанных изображений.
     */
    void recordImageDownload();

    /**
     * Увеличивает счетчик успешных архивов.
     */
    void recordArchiveSuccess();

    /**
     * Увеличивает счетчик ошибок создания архивов.
     */
    void recordArchiveFailure();

    /**
     * Увеличивает счетчик активных загрузок.
     */
    void incrementActiveDownloads();

    /**
     * Уменьшает счетчик активных загрузок.
     */
    void decrementActiveDownloads();
}
