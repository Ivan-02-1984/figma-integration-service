package com.company.figmaintegrationservice.service;

import com.company.figmaintegrationservice.config.ArchiveSettings;
import com.company.figmaintegrationservice.dto.FigmaExportDto;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Интерфейс для сервиса создания архивов из экспортированных данных Figma.
 */
public interface IFigmaArchiveService {
    /**
     * Создает ZIP архив и записывает его в поток.
     *
     * @param exportDto данные для экспорта
     * @param outputStream поток для записи архива
     * @param taskId идентификатор задачи (для отслеживания прогресса)
     * @param taskService сервис для обновления прогресса
     * @param settings настройки архива
     * @throws IOException если произошла ошибка при создании архива
     */
    void streamArchive(FigmaExportDto exportDto, OutputStream outputStream,
                      String taskId, ITaskService taskService, ArchiveSettings settings) throws IOException;
}
