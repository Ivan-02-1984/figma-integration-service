package com.company.figmaintegrationservice.service.impl;

import com.company.figmaintegrationservice.service.IMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class MetricsService implements IMetricsService {

    private final MeterRegistry meterRegistry;
    private final Timer archiveCreationTimer;
    private final Counter imageDownloadCounter;
    private final Counter archiveSuccessCounter;
    private final Counter archiveFailureCounter;
    private final AtomicLong activeDownloads;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.archiveCreationTimer = Timer.builder("figma.archive.creation.time")
                .description("Время создания архива")
                .register(meterRegistry);

        this.imageDownloadCounter = Counter.builder("figma.image.downloads")
                .description("Количество скачанных картинок")
                .register(meterRegistry);

        this.archiveSuccessCounter = Counter.builder("figma.archive.success")
                .description("Успешные архивы")
                .register(meterRegistry);

        this.archiveFailureCounter = Counter.builder("figma.archive.failures")
                .description("Ошибки создания архивов")
                .register(meterRegistry);

        this.activeDownloads = meterRegistry.gauge("figma.active.downloads", new AtomicLong(0),
                AtomicLong::doubleValue);
    }

    @Override
    public <T> T measureArchiveCreation(Callable<T> action) throws Exception {
        try {
            return archiveCreationTimer.recordCallable(action);
        } catch (Exception e) {
            log.error("Ошибка при измерении времени создания архива", e);
            throw e;
        }
    }

    @Override
    public void recordImageDownload() {
        imageDownloadCounter.increment();
    }

    @Override
    public void recordArchiveSuccess() {
        archiveSuccessCounter.increment();
    }

    @Override
    public void recordArchiveFailure() {
        archiveFailureCounter.increment();
    }

    @Override
    public void incrementActiveDownloads() {
        activeDownloads.incrementAndGet();
    }

    @Override
    public void decrementActiveDownloads() {
        activeDownloads.decrementAndGet();
    }
}