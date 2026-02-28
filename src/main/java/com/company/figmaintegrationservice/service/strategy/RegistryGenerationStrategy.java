package com.company.figmaintegrationservice.service.strategy;

import com.company.figmaintegrationservice.dto.FigmaExportDto;

import java.io.IOException;

/**
 * Интерфейс стратегии для генерации реестров в различных форматах.
 * Применяет принцип Open/Closed - можно добавлять новые форматы без изменения существующего кода.
 */
public interface RegistryGenerationStrategy {

    /**
     * Генерирует реестр в соответствующем формате.
     *
     * @param exportDto данные для экспорта
     * @return массив байтов сгенерированного реестра
     * @throws IOException если произошла ошибка при генерации
     */
    byte[] generate(FigmaExportDto exportDto) throws IOException;

    /**
     * Возвращает имя формата, который поддерживает эта стратегия.
     *
     * @return имя формата (например, "csv", "excel")
     */
    String getFormatName();

    /**
     * Возвращает имя файла для реестра.
     *
     * @return имя файла (например, "index.csv", "index.xlsx")
     */
    String getFileName();
}
