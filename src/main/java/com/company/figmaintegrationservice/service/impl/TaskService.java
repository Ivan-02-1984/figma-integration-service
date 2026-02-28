package com.company.figmaintegrationservice.service.impl;

import com.company.figmaintegrationservice.config.ArchiveSettings;
import com.company.figmaintegrationservice.dto.FigmaExportDto;
import com.company.figmaintegrationservice.service.ITaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TaskService implements ITaskService {

    private final Map<String, ITaskService.TaskProgress> tasks = new ConcurrentHashMap<>();
    private final Map<String, FigmaExportDto> exportData = new ConcurrentHashMap<>();
    private final Map<String, ArchiveSettings> archiveSettings = new ConcurrentHashMap<>();

    @Override
    public String createTask() {
        String taskId = UUID.randomUUID().toString();
        tasks.put(taskId, new ITaskService.TaskProgress(0, 0, "PENDING"));
        return taskId;
    }

    @Override
    public void updateProgress(String taskId, int current, int total, String status) {
        ITaskService.TaskProgress progress = tasks.get(taskId);
        if (progress != null) {
            progress.setCurrent(current);
            progress.setTotal(total);
            progress.setStatus(status);
            log.debug("Задача {}: {}/{} - {}", taskId, current, total, status);
        }
    }

    @Override
    public ITaskService.TaskProgress getProgress(String taskId) {
        return tasks.getOrDefault(taskId, new ITaskService.TaskProgress(0, 0, "NOT_FOUND"));
    }

    public void setExportData(String taskId, FigmaExportDto exportDto) {
        exportData.put(taskId, exportDto);
    }

    public FigmaExportDto getExportData(String taskId) {
        return exportData.get(taskId);
    }

    public void setArchiveSettings(String taskId, ArchiveSettings settings) {
        archiveSettings.put(taskId, settings);
    }

    public ArchiveSettings getArchiveSettings(String taskId) {
        return archiveSettings.getOrDefault(taskId, new ArchiveSettings());
    }

    public void removeTask(String taskId) {
        tasks.remove(taskId);
        exportData.remove(taskId);
        archiveSettings.remove(taskId);
    }

}