package com.company.figmaintegrationservice.controller;

import com.company.figmaintegrationservice.dto.FigmaExportDto;
import com.company.figmaintegrationservice.service.IFigmaExportService;
import com.company.figmaintegrationservice.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/figma")
@RequiredArgsConstructor
public class FigmaExportController {

    private final IFigmaExportService exportService;

    @GetMapping("/export")
    public StreamingResponseBody export(@RequestParam String token, @RequestParam String fileId) {
        return outputStream -> {
            try {
                FigmaExportDto exportDto = exportService.exportFile(token, fileId);

                try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
                    zip.putNextEntry(new ZipEntry("figma.json"));
                    zip.write(JsonUtils.toJsonBytes(exportDto));
                    zip.closeEntry();
                }
            } catch (Exception e) {
                outputStream.write(("Ошибка: " + e.getMessage()).getBytes());
            }
        };
    }

    @lombok.AllArgsConstructor
    @lombok.Getter
    static class ErrorResponse {
        private final String message;
    }
}