package com.company.figmaintegrationservice.service;

import com.company.figmaintegrationservice.dto.FigmaExportDto;

import java.io.IOException;

/**
 * Интерфейс для сервиса генерации реестров.
 */
public interface IRegistryService {
    /**
     * Генерирует реестр в указанном формате.
     *
     * @param exportDto данные для экспорта
     * @param format формат (csv, excel и т.д.)
     * @return массив байтов сгенерированного реестра
     * @throws IOException если произошла ошибка при генерации
     * @throws IllegalArgumentException если формат не поддерживается
     */
    byte[] generateRegistry(FigmaExportDto exportDto, String format) throws IOException;

    /**
     * Возвращает имя файла для указанного формата.
     */
    String getFileName(String format);

    /**
     * Проверяет, поддерживается ли указанный формат.
     */
    boolean isFormatSupported(String format);
}
