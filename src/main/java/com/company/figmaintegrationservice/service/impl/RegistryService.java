package com.company.figmaintegrationservice.service.impl;

import com.company.figmaintegrationservice.dto.FigmaExportDto;
import com.company.figmaintegrationservice.service.IRegistryService;
import com.company.figmaintegrationservice.service.strategy.RegistryGenerationStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис для управления стратегиями генерации реестров.
 * Применяет Dependency Inversion Principle - зависит от абстракции (RegistryGenerationStrategy).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistryService implements IRegistryService {

    private final List<RegistryGenerationStrategy> strategies;
    private Map<String, RegistryGenerationStrategy> strategyMap;

    /**
     * Инициализирует карту стратегий после внедрения зависимостей.
     */
    @PostConstruct
    public void init() {
        strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        strategy -> strategy.getFormatName().toLowerCase(),
                        Function.identity()
                ));
        log.info("📋 Зарегистрировано стратегий генерации реестров: {}", strategyMap.keySet());
    }

    /**
     * Генерирует реестр в указанном формате.
     *
     * @param exportDto данные для экспорта
     * @param format формат (csv, excel и т.д.)
     * @return массив байтов сгенерированного реестра
     * @throws IOException если произошла ошибка при генерации
     * @throws IllegalArgumentException если формат не поддерживается
     */
    @Override
    public byte[] generateRegistry(FigmaExportDto exportDto, String format) throws IOException {
        String cleanFormat = format.trim().toLowerCase();
        RegistryGenerationStrategy strategy = strategyMap.get(cleanFormat);

        if (strategy == null) {
            throw new IllegalArgumentException("Формат реестра не поддерживается: " + format +
                    ". Доступные форматы: " + strategyMap.keySet());
        }

        log.info("📊 Генерация реестра в формате {}: {} текстов, {} изображений",
                format, exportDto.getTexts().size(), exportDto.getImages().size());

        return strategy.generate(exportDto);
    }

    /**
     * Возвращает имя файла для указанного формата.
     */
    @Override
    public String getFileName(String format) {
        String cleanFormat = format.trim().toLowerCase();
        RegistryGenerationStrategy strategy = strategyMap.get(cleanFormat);
        return strategy != null ? strategy.getFileName() : "index." + format;
    }

    /**
     * Проверяет, поддерживается ли указанный формат.
     */
    @Override
    public boolean isFormatSupported(String format) {
        return strategyMap.containsKey(format.trim().toLowerCase());
    }
}
