package com.company.figmaintegrationservice.service;

import java.io.IOException;

/**
 * Интерфейс для сервиса загрузки изображений.
 */
public interface IImageDownloadService {
    /**
     * Скачивает изображение по URL с повторными попытками.
     *
     * @param imageUrl URL изображения
     * @param maxRetries максимальное количество попыток
     * @return массив байтов изображения или null, если не удалось скачать
     * @throws IOException если произошла ошибка при загрузке
     * @throws InterruptedException если поток был прерван
     */
    byte[] downloadImageWithRetry(String imageUrl, int maxRetries) throws IOException, InterruptedException;
}
