package com.company.figmaintegrationservice.service.impl;

import com.company.figmaintegrationservice.dto.FigmaImageDto;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис для работы с путями изображений в архиве.
 * Отвечает только за формирование путей и имен файлов (Single Responsibility Principle).
 */
@Service
public class ImagePathService {

    /**
     * Формирует уникальное имя для записи в ZIP.
     */
    public String buildEntryName(FigmaImageDto image, Map<String, AtomicInteger> nameCounter) {
        if (image == null) return "unknown/unknown.png";

        String path = buildFolderPath(image);
        String baseName = buildBaseImageName(image);

        String key = path + "/" + baseName;
        AtomicInteger counter = nameCounter.computeIfAbsent(key, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();

        String extension = ".png";
        return count == 1 ? key + extension : key + "_" + count + extension;
    }

    /**
     * Строит путь к папке на основе иерархии страница/фрейм.
     */
    public String buildFolderPath(FigmaImageDto image) {
        if (image == null) return "Unknown";

        StringBuilder path = new StringBuilder();

        if (image.getPageName() != null && !image.getPageName().isBlank()) {
            path.append(sanitizeName(image.getPageName()));
        } else {
            path.append("UnknownPage");
        }

        if (image.getFrameName() != null && !image.getFrameName().isBlank()) {
            path.append("/").append(sanitizeName(image.getFrameName()));
        }

        return path.toString();
    }

    /**
     * Формирует базовое имя файла с nodeId.
     */
    public String buildBaseImageName(FigmaImageDto image) {
        if (image == null) return "image";

        StringBuilder name = new StringBuilder();

        if (image.getNodeName() != null && !image.getNodeName().isBlank()) {
            name.append(sanitizeName(image.getNodeName()));
        } else {
            name.append("image");
        }

        if (image.getNodeId() != null && !image.getNodeId().isBlank()) {
            name.append("_id_").append(sanitizeName(image.getNodeId()));
        }

        return name.toString();
    }

    /**
     * Очищает имя от недопустимых символов.
     */
    public String sanitizeName(String name) {
        if (name == null || name.isBlank()) return "unknown";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
