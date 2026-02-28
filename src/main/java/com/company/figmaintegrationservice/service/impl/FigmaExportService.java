package com.company.figmaintegrationservice.service.impl;

import com.company.figmaintegrationservice.client.IFigmaClient;
import com.company.figmaintegrationservice.client.dto.FigmaFileResponse;
import com.company.figmaintegrationservice.client.dto.FigmaNodesResponse;
import com.company.figmaintegrationservice.client.dto.PageNode;
import com.company.figmaintegrationservice.config.ArchiveSettings;
import com.company.figmaintegrationservice.dto.FigmaExportDto;
import com.company.figmaintegrationservice.dto.FigmaImageDto;
import com.company.figmaintegrationservice.dto.FigmaTextDto;
import com.company.figmaintegrationservice.service.IFigmaExportService;
import com.company.figmaintegrationservice.service.impl.NodeTraversalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FigmaExportService implements IFigmaExportService {

    private final IFigmaClient figmaClient;
    private final NodeTraversalService nodeTraversalService;

    /**
     * Стандартная выгрузка всего файла
     */
    public FigmaExportDto exportFile(String token, String fileId) {
        FigmaFileResponse fileResponse = figmaClient.getFile(token, fileId).block();
        if (fileResponse == null || fileResponse.getDocument() == null) {
            throw new RuntimeException("Figma document is empty");
        }

        List<PageNode> pages = fileResponse.getDocument().getChildren();

        List<FigmaTextDto> texts = new ArrayList<>();
        List<FigmaImageDto> images = new ArrayList<>();

        if (pages != null) {
            for (PageNode page : pages) {
                if (page.getChildren() != null) {
                    nodeTraversalService.traverseNodes(page.getChildren(), page.getName(), "", texts, images);
                }
            }
        }

        log.info("✅ Экспорт завершен: {} текстов, {} изображений", texts.size(), images.size());
        return new FigmaExportDto(fileResponse.getVersion(), texts, images);
    }

    /**
     * Выгрузка только выбранных узлов по ID
     */
    public FigmaExportDto exportSelectedNodes(String token, String fileId, String nodeIds, int depth) {
        log.info("🎯 Выгрузка узлов: {} с глубиной {}", nodeIds, depth);

        FigmaNodesResponse nodesResponse = figmaClient.getNodes(token, fileId, nodeIds, depth).block();
        if (nodesResponse == null || nodesResponse.getNodes() == null) {
            throw new RuntimeException("Figma nodes response is empty");
        }

        List<FigmaTextDto> texts = new ArrayList<>();
        List<FigmaImageDto> images = new ArrayList<>();

        for (Map.Entry<String, FigmaNodesResponse.NodeWrapper> entry : nodesResponse.getNodes().entrySet()) {
            FigmaNodesResponse.NodeWrapper wrapper = entry.getValue();
            if (wrapper != null && wrapper.getDocument() != null) {
                nodeTraversalService.traverseNode(wrapper.getDocument(), "Selected", "", texts, images);
            }
        }

        log.info("✅ Выборочный экспорт завершен: {} текстов, {} изображений", texts.size(), images.size());
        return new FigmaExportDto("v1", texts, images);
    }

    /**
     * Универсальный метод экспорта с учётом настроек
     */
    public FigmaExportDto exportWithSettings(String token, String fileId, ArchiveSettings settings) {
        if (settings != null && settings.isSelectedMode()) {
            return exportSelectedNodes(token, fileId, settings.getNodeIds(), settings.getNodeDepth());
        } else {
            return exportFile(token, fileId);
        }
    }

}