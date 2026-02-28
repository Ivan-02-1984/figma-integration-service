package com.company.figmaintegrationservice.service.impl;

import com.company.figmaintegrationservice.config.ArchiveSettings;
import com.company.figmaintegrationservice.dto.FigmaExportDto;
import com.company.figmaintegrationservice.dto.FigmaImageDto;
import com.company.figmaintegrationservice.service.IFigmaArchiveService;
import com.company.figmaintegrationservice.service.IImageDownloadService;
import com.company.figmaintegrationservice.service.IMetricsService;
import com.company.figmaintegrationservice.service.IRegistryService;
import com.company.figmaintegrationservice.service.ITaskService;
import com.company.figmaintegrationservice.service.impl.ImagePathService;
import com.company.figmaintegrationservice.service.impl.ZipArchiveService;
import com.company.figmaintegrationservice.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FigmaArchiveService implements IFigmaArchiveService, InitializingBean {

    private final IMetricsService metricsService;
    private final IRegistryService registryService;
    private final IImageDownloadService imageDownloadService;
    private final ZipArchiveService zipArchiveService;
    private final ImagePathService imagePathService;
    private final ExecutorService virtualThreadExecutor;

    @Value("${figma.archive.max-images:5000}")
    private int maxImages;

    @Value("${figma.archive.delay-between-requests:300}")
    private int delayBetweenRequests;

    @Value("${figma.archive.max-concurrent-downloads:8}")
    private int maxConcurrentDownloads;

    @Value("${archive.include-registry:true}")
    private boolean defaultIncludeRegistry;

    @Value("${archive.registry-formats:csv}")
    private String[] defaultRegistryFormats;

    // Семафор для ограничения количества одновременных загрузок
    private Semaphore downloadSemaphore;

    @Override
    public void afterPropertiesSet() {
        downloadSemaphore = new Semaphore(maxConcurrentDownloads);
        log.info("🚦 Семафор инициализирован: максимум {} одновременных загрузок", maxConcurrentDownloads);
    }

    @Override
    public void streamArchive(FigmaExportDto exportDto, OutputStream outputStream,
                              String taskId, ITaskService taskService, ArchiveSettings settings) throws IOException {

        Map<String, AtomicInteger> nameCounter = new HashMap<>();
        List<FigmaImageDto> images = exportDto.getImages() != null ? exportDto.getImages() : new ArrayList<>();
        AtomicInteger completed = new AtomicInteger(0);
        int total = images.size();

        if (images.size() > maxImages) {
            log.warn("⚠️ Слишком много картинок ({}), ограничиваем до {}", images.size(), maxImages);
            images = images.subList(0, maxImages);
        }

        try (ZipOutputStream zip = zipArchiveService.createZipArchive(outputStream)) {

            // 1. JSON метаданные
            byte[] jsonData = JsonUtils.toJsonBytes(exportDto);
            if (jsonData != null && jsonData.length > 0) {
                zipArchiveService.addToZip(zip, "figma.json", jsonData);
            } else {
                log.warn("⚠️ JSON метаданные пустые, пропускаем");
            }

            // 2. Реестры - используем настройки из параметра settings (приоритет)
            boolean useRegistry = settings != null ? settings.isIncludeRegistry() : defaultIncludeRegistry;
            String[] formats = settings != null && settings.getRegistryFormats() != null ?
                    settings.getRegistryFormats() : defaultRegistryFormats;

            if (useRegistry && formats != null) {
                for (String format : formats) {
                    try {
                        if (!registryService.isFormatSupported(format)) {
                            log.warn("⚠️ Формат реестра не поддерживается: {}", format);
                            continue;
                        }

                        byte[] registryData = registryService.generateRegistry(exportDto, format);
                        String fileName = registryService.getFileName(format);

                        if (registryData != null && registryData.length > 0) {
                            zipArchiveService.addToZip(zip, fileName, registryData);
                            log.info("📊 Реестр {} добавлен в архив ({} байт)",
                                    fileName, registryData.length);
                        } else {
                            log.warn("⚠️ Реестр {} пустой, пропускаем", fileName);
                        }

                    } catch (Exception e) {
                        log.error("❌ Ошибка при создании реестра {}: {}", format, e.getMessage());
                    }
                }
            }

            if (images.isEmpty()) {
                log.info("🚀 Нет картинок для загрузки");
            } else {
                log.info("🚀 Запускаем загрузку {} картинок: максимум {} параллельно, задержка {} мс",
                        images.size(), maxConcurrentDownloads, delayBetweenRequests);
                long startTime = System.currentTimeMillis();

                List<CompletableFuture<Void>> futures = new ArrayList<>();

                // 3. Картинки (параллельная загрузка с ограничением через семафор)
                for (FigmaImageDto image : images) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            // Получаем разрешение от семафора (ограничение параллельных загрузок)
                            downloadSemaphore.acquire();
                            
                            try {
                                metricsService.incrementActiveDownloads();

                                // Дополнительная задержка между стартами загрузок
                                if (delayBetweenRequests > 0) {
                                    Thread.sleep(delayBetweenRequests);
                                }

                                if (image == null || image.getImageUrl() == null || image.getImageUrl().isBlank()) {
                                    log.warn("⚠️ Пропуск: некорректные данные изображения");
                                    return;
                                }

                                String entryName = imagePathService.buildEntryName(image, nameCounter);
                                byte[] imageData = imageDownloadService.downloadImageWithRetry(image.getImageUrl(), 3);

                                if (imageData != null && imageData.length > 0) {
                                    synchronized (zip) {
                                        zipArchiveService.addToZip(zip, entryName, imageData);
                                    }
                                    metricsService.recordImageDownload();
                                } else {
                                    log.warn("⚠️ Изображение {} пустое, пропускаем", image.getImageUrl());
                                }

                                int current = completed.incrementAndGet();

                                if (taskService != null && taskId != null) {
                                    taskService.updateProgress(taskId, current, total, "DOWNLOADING");
                                }

                                if (current % 100 == 0) {
                                    log.info("📊 Прогресс: {}/{} картинок ({}%)",
                                            current, total, (current * 100 / total));
                                }
                            } finally {
                                // Освобождаем семафор
                                downloadSemaphore.release();
                                metricsService.decrementActiveDownloads();
                            }

                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.error("❌ Поток прерван при загрузке изображения");
                        } catch (Exception e) {
                            log.error("❌ Ошибка в потоке для {}: {}",
                                    image != null ? image.getImageUrl() : "null",
                                    e.getMessage() != null ? e.getMessage() : "null");
                        }
                    }, virtualThreadExecutor);

                    futures.add(future);
                }

                // Ждем завершения всех потоков
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                long duration = System.currentTimeMillis() - startTime;
                double speed = images.size() * 1000.0 / duration;
                log.info("✅ Картинки загружены за {} мс, {} картинок, средняя скорость: {:.2f} картинок/сек",
                        duration, images.size(), speed);
            }

            zipArchiveService.finishZip(zip);

            if (taskService != null && taskId != null) {
                taskService.updateProgress(taskId, total, total, "COMPLETED");
            }

        } catch (Exception e) {
            log.error("❌ Критическая ошибка создания архива", e);
            throw new IOException("Ошибка создания архива: " + e.getMessage(), e);
        }
    }

}