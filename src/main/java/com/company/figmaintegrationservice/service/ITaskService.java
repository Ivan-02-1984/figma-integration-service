package com.company.figmaintegrationservice.service;

import com.company.figmaintegrationservice.config.ArchiveSettings;
import com.company.figmaintegrationservice.dto.FigmaExportDto;

/**
 * Интерфейс для сервиса управления задачами экспорта.
 */
public interface ITaskService {
    /**
     * Создает новую задачу и возвращает её идентификатор.
     */
    String createTask();

    /**
     * Обновляет прогресс выполнения задачи.
     */
    void updateProgress(String taskId, int current, int total, String status);

    /**
     * Получает текущий прогресс задачи.
     */
    TaskProgress getProgress(String taskId);

    /**
     * Сохраняет данные экспорта для задачи.
     */
    void setExportData(String taskId, FigmaExportDto exportDto);

    /**
     * Получает данные экспорта для задачи.
     */
    FigmaExportDto getExportData(String taskId);

    /**
     * Сохраняет настройки архива для задачи.
     */
    void setArchiveSettings(String taskId, ArchiveSettings settings);

    /**
     * Получает настройки архива для задачи.
     */
    ArchiveSettings getArchiveSettings(String taskId);

    /**
     * Удаляет задачу и все связанные данные.
     */
    void removeTask(String taskId);

    /**
     * DTO для прогресса задачи.
     */
    class TaskProgress {
        private int current;
        private int total;
        private String status;
        private byte[] result;

        public TaskProgress(int current, int total, String status) {
            this.current = current;
            this.total = total;
            this.status = status;
        }

        public TaskProgress() {
        }

        public int getCurrent() {
            return current;
        }

        public void setCurrent(int current) {
            this.current = current;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public byte[] getResult() {
            return result;
        }

        public void setResult(byte[] result) {
            this.result = result;
        }
    }
}
