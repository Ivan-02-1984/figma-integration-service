package com.company.figmaintegrationservice.service.strategy;

import com.company.figmaintegrationservice.dto.FigmaExportDto;
import com.company.figmaintegrationservice.dto.FigmaImageDto;
import com.company.figmaintegrationservice.dto.FigmaTextDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Стратегия генерации Excel реестра.
 */
@Slf4j
@Component
public class ExcelRegistryStrategy implements RegistryGenerationStrategy {

    private static final String[] HEADERS = {
            "nodeId", "type", "pageName", "frameName", "nodeName",
            "text", "imageUrl", "path", "extractedText"
    };

    @Override
    public byte[] generate(FigmaExportDto exportDto) throws IOException {
        log.info("📊 Генерация Excel: {} текстов, {} изображений",
                exportDto.getTexts().size(), exportDto.getImages().size());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Figma Export");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle pageSeparatorStyle = workbook.createCellStyle();
            pageSeparatorStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            pageSeparatorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            String currentPage = null;

            List<RegistryRow> allRows = new ArrayList<>();
            for (FigmaTextDto text : exportDto.getTexts()) {
                allRows.add(new RegistryRow(text));
            }
            for (FigmaImageDto image : exportDto.getImages()) {
                allRows.add(new RegistryRow(image));
            }

            allRows.sort(Comparator
                    .comparing((RegistryRow r) -> r.pageName)
                    .thenComparing(r -> r.frameName, Comparator.nullsLast(String::compareTo))
                    .thenComparing(r -> r.nodeName));

            for (RegistryRow row : allRows) {
                if (!row.pageName.equals(currentPage)) {
                    currentPage = row.pageName;

                    Row separatorRow = sheet.createRow(rowNum++);
                    for (int i = 0; i < HEADERS.length; i++) {
                        Cell cell = separatorRow.createCell(i);
                        if (i == 0) {
                            cell.setCellValue("=== " + currentPage + " ===");
                        }
                        cell.setCellStyle(pageSeparatorStyle);
                    }
                }

                Row excelRow = sheet.createRow(rowNum++);
                excelRow.createCell(0).setCellValue(row.nodeId);
                excelRow.createCell(1).setCellValue(row.type);
                excelRow.createCell(2).setCellValue(row.pageName);
                excelRow.createCell(3).setCellValue(row.frameName);
                excelRow.createCell(4).setCellValue(row.nodeName);
                excelRow.createCell(5).setCellValue(row.text);
                excelRow.createCell(6).setCellValue(row.imageUrl);
                excelRow.createCell(7).setCellValue(row.path);
                excelRow.createCell(8).setCellValue(row.extractedText);
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            log.info("✅ Excel готов: {} строк", rowNum - 1);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();

            return baos.toByteArray();
        }
    }

    @Override
    public String getFormatName() {
        return "excel";
    }

    @Override
    public String getFileName() {
        return "index.xlsx";
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
    }

    private static String safeString(String s) {
        return s != null ? s : "";
    }
}
