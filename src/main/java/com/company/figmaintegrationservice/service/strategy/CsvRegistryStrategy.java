package com.company.figmaintegrationservice.service.strategy;

import com.company.figmaintegrationservice.dto.FigmaExportDto;
import com.company.figmaintegrationservice.dto.FigmaImageDto;
import com.company.figmaintegrationservice.dto.FigmaTextDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Стратегия генерации CSV реестра.
 */
@Slf4j
@Component
public class CsvRegistryStrategy implements RegistryGenerationStrategy {

    private static final String[] HEADERS = {
            "nodeId", "type", "pageName", "frameName", "nodeName",
            "text", "imageUrl", "path", "extractedText"
    };

    @Override
    public byte[] generate(FigmaExportDto exportDto) throws IOException {
        log.info("📊 Генерация CSV: {} текстов, {} изображений",
                exportDto.getTexts().size(), exportDto.getImages().size());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(0xEF);
        baos.write(0xBB);
        baos.write(0xBF);

        OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);

        writer.write(String.join(";", HEADERS));
        writer.write("\n");

        Map<String, List<RegistryRow>> groupedByPage = new TreeMap<>();

        for (FigmaTextDto text : exportDto.getTexts()) {
            String pageKey = text.getPageName() != null ? text.getPageName() : "Без страницы";
            groupedByPage.computeIfAbsent(pageKey, k -> new ArrayList<>())
                    .add(new RegistryRow(text));
        }

        for (FigmaImageDto image : exportDto.getImages()) {
            String pageKey = image.getPageName() != null ? image.getPageName() : "Без страницы";
            groupedByPage.computeIfAbsent(pageKey, k -> new ArrayList<>())
                    .add(new RegistryRow(image));
        }

        int totalRows = 0;

        for (Map.Entry<String, List<RegistryRow>> page : groupedByPage.entrySet()) {
            List<RegistryRow> pageRows = page.getValue();

            pageRows.sort(Comparator
                    .comparing(RegistryRow::getFrameName, Comparator.nullsLast(String::compareTo))
                    .thenComparing(RegistryRow::getNodeName));

            for (RegistryRow row : pageRows) {
                writer.write(escapeCsv(row.nodeId)); writer.write(";");
                writer.write(escapeCsv(row.type)); writer.write(";");
                writer.write(escapeCsv(row.pageName)); writer.write(";");
                writer.write(escapeCsv(row.frameName)); writer.write(";");
                writer.write(escapeCsv(row.nodeName)); writer.write(";");
                writer.write(escapeCsv(row.text)); writer.write(";");
                writer.write(escapeCsv(row.imageUrl)); writer.write(";");
                writer.write(escapeCsv(row.path)); writer.write(";");
                writer.write(escapeCsv(row.extractedText));
                writer.write("\n");
                totalRows++;
            }

            writer.write("\n");
        }

        writer.flush();

        log.info("✅ CSV готов: {} строк, {} страниц", totalRows, groupedByPage.size());
        return baos.toByteArray();
    }

    @Override
    public String getFormatName() {
        return "csv";
    }

    @Override
    public String getFileName() {
        return "index.csv";
    }

    private String escapeCsv(String value) {
        if (value == null || value.isEmpty()) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains(",")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static class RegistryRow {
        String nodeId;
        String type;
        String pageName;
        String frameName;
        String nodeName;
        String text;
        String imageUrl;
        String path;
        String extractedText;

        RegistryRow(FigmaTextDto text) {
            this.nodeId = safeString(text.getNodeId());
            this.type = "TEXT";
            this.pageName = safeString(text.getPageName());
            this.frameName = safeString(text.getFrameName());
            this.nodeName = safeString(text.getNodeName());
            this.text = safeString(text.getText());
            this.imageUrl = "";
            this.path = safeString(text.getPath());
            this.extractedText = "";
        }

        RegistryRow(FigmaImageDto image) {
            this.nodeId = safeString(image.getNodeId());
            this.type = "RECTANGLE";
            this.pageName = safeString(image.getPageName());
            this.frameName = safeString(image.getFrameName());
            this.nodeName = safeString(image.getNodeName());
            this.text = "";
            this.imageUrl = safeString(image.getImageUrl());
            this.path = safeString(image.getPath());
            this.extractedText = safeString(image.getExtractedText());
        }

        String getFrameName() { return frameName; }
        String getNodeName() { return nodeName; }
    }

    private static String safeString(String s) {
        return s != null ? s : "";
    }
}
