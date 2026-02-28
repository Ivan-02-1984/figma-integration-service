package com.company.figmaintegrationservice.service;

import com.company.figmaintegrationservice.config.ArchiveSettings;
import com.company.figmaintegrationservice.dto.FigmaExportDto;

/**
 * Интерфейс для сервиса экспорта данных из Figma.
 */
public interface IFigmaExportService {
    /**
     * Стандартная выгрузка всего файла.
     */
    FigmaExportDto exportFile(String token, String fileId);

    /**
     * Выгрузка только выбранных узлов по ID.
     */
    FigmaExportDto exportSelectedNodes(String token, String fileId, String nodeIds, int depth);

    /**
     * Универсальный метод экспорта с учётом настроек.
     */
    FigmaExportDto exportWithSettings(String token, String fileId, ArchiveSettings settings);
}
