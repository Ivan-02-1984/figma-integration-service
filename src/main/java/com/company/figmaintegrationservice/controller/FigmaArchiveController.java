package com.company.figmaintegrationservice.controller;

import com.company.figmaintegrationservice.config.ArchiveSettings;
import com.company.figmaintegrationservice.dto.FigmaExportDto;
import com.company.figmaintegrationservice.service.IFigmaArchiveService;
import com.company.figmaintegrationservice.service.IFigmaExportService;
import com.company.figmaintegrationservice.service.IMetricsService;
import com.company.figmaintegrationservice.service.ITaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/figma")
public class FigmaArchiveController {

    private final IFigmaExportService exportService;
    private final IFigmaArchiveService archiveService;
    private final ITaskService taskService;
    private final IMetricsService metricsService;

    @GetMapping("/archive/start")
    public ResponseEntity<StartResponse> startArchive(
            @RequestParam String token,
            @RequestParam String fileId,
            @RequestParam(required = false, defaultValue = "full") String exportMode,
            @RequestParam(required = false) String nodeIds,
            @RequestParam(required = false, defaultValue = "2") int nodeDepth,
            @RequestParam(required = false, defaultValue = "true") boolean includeRegistry,
            @RequestParam(required = false, defaultValue = "csv") String registryFormats) {

        String taskId = taskService.createTask();

        // Сохраняем настройки
        ArchiveSettings settings = new ArchiveSettings();
        settings.setIncludeRegistry(includeRegistry);
        settings.setRegistryFormats(registryFormats.split(","));
        settings.setExportMode(exportMode);
        settings.setNodeIds(nodeIds);
        settings.setNodeDepth(nodeDepth);

        taskService.setArchiveSettings(taskId, settings);

        CompletableFuture.runAsync(() -> {
            try {
                taskService.updateProgress(taskId, 0, 1, "EXPORTING");

                // Используем универсальный метод экспорта
                FigmaExportDto exportDto = exportService.exportWithSettings(token, fileId, settings);

                taskService.setExportData(taskId, exportDto);
                taskService.updateProgress(taskId, 0, exportDto.getImages().size(), "READY");
                log.info("✅ Экспорт завершен: {} текстов, {} изображений",
                        exportDto.getTexts().size(), exportDto.getImages().size());

            } catch (Exception e) {
                log.error("❌ Ошибка при экспорте", e);
                taskService.updateProgress(taskId, 0, 1, "ERROR: " + e.getMessage());
            }
        });

        return ResponseEntity.ok(new StartResponse(taskId));
    }

    @GetMapping("/archive/status/{taskId}")
    public ResponseEntity<ITaskService.TaskProgress> getStatus(@PathVariable String taskId) {
        return ResponseEntity.ok(taskService.getProgress(taskId));
    }

    @GetMapping("/archive/download/{taskId}")
    public ResponseEntity<StreamingResponseBody> downloadArchive(@PathVariable String taskId) {
        FigmaExportDto exportDto = taskService.getExportData(taskId);
        ArchiveSettings settings = taskService.getArchiveSettings(taskId);

        if (exportDto == null) {
            return ResponseEntity.notFound().build();
        }

        log.info("📦 Начинаем потоковую передачу архива для задачи {}, режим: {}, реестр: {}, форматы: {}",
                taskId, settings.getExportMode(), settings.isIncludeRegistry(), String.join(",", settings.getRegistryFormats()));

        taskService.updateProgress(taskId, 0, exportDto.getImages().size(), "STREAMING");

        StreamingResponseBody stream = outputStream -> {
            try {
                archiveService.streamArchive(exportDto, outputStream, taskId, taskService, settings);
                taskService.updateProgress(taskId, exportDto.getImages().size(),
                        exportDto.getImages().size(), "COMPLETED");
                log.info("✅ Архив успешно передан для задачи {}", taskId);

            } catch (Exception e) {
                log.error("❌ Ошибка при стриминге архива для задачи {}", taskId, e);
                taskService.updateProgress(taskId, 0, 1, "ERROR: " + e.getMessage());
                throw e;
            } finally {
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(300000);
                        taskService.removeTask(taskId);
                    } catch (InterruptedException ignored) {}
                });
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"figma-archive.zip\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                .body(stream);
    }

    @GetMapping("/metrics")
    public ResponseEntity<String> metrics() {
        return ResponseEntity.ok("Metrics available at /actuator/prometheus");
    }

    @lombok.AllArgsConstructor
    @lombok.Getter
    static class StartResponse {
        private String taskId;
    }
}