package com.company.figmaintegrationservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Сервис для работы с ZIP архивами.
 * Отвечает только за операции с ZIP (Single Responsibility Principle).
 */
@Slf4j
@Service
public class ZipArchiveService {

    /**
     * Создает ZIP архив и возвращает ZipOutputStream для записи.
     */
    public ZipOutputStream createZipArchive(OutputStream outputStream) {
        return new ZipOutputStream(new BufferedOutputStream(outputStream));
    }

    /**
     * Добавляет файл в ZIP архив.
     *
     * @param zip ZIP поток
     * @param fileName имя файла
     * @param data данные файла
     * @throws IOException если произошла ошибка при добавлении
     */
    public void addToZip(ZipOutputStream zip, String fileName, byte[] data) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            log.warn("⚠️ Пропуск: имя файла пустое");
            return;
        }
        if (data == null || data.length == 0) {
            log.warn("⚠️ Пропуск {}: данные отсутствуют", fileName);
            return;
        }
        try {
            zip.putNextEntry(new ZipEntry(fileName));
            zip.write(data);
            zip.closeEntry();
            zip.flush();
        } catch (Exception e) {
            log.error("❌ Ошибка при добавлении {} в ZIP: {}", fileName, e.getMessage());
            throw e;
        }
    }

    /**
     * Завершает создание ZIP архива.
     */
    public void finishZip(ZipOutputStream zip) throws IOException {
        zip.finish();
        zip.flush();
    }
}
