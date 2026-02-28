package com.company.figmaintegrationservice.service.impl;

import com.company.figmaintegrationservice.client.dto.FigmaNode;
import com.company.figmaintegrationservice.dto.FigmaImageDto;
import com.company.figmaintegrationservice.dto.FigmaTextDto;
import com.company.figmaintegrationservice.mapper.FigmaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для обхода дерева узлов Figma.
 * Отвечает только за обход и извлечение данных из узлов (Single Responsibility Principle).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeTraversalService {

    private final FigmaMapper figmaMapper;

    /**
     * Обходит дерево узлов и собирает тексты и изображения.
     *
     * @param node корневой узел для обхода
     * @param pageName имя страницы
     * @param path текущий путь в иерархии
     * @param texts список для накопления текстов
     * @param images список для накопления изображений
     */
    public void traverseNode(FigmaNode node, String pageName, String path,
                            List<FigmaTextDto> texts, List<FigmaImageDto> images) {
        if (node == null) return;

        String currentPath = path.isEmpty() ? node.getName() : path + " → " + node.getName();

        if ("TEXT".equals(node.getType()) && node.getCharacters() != null) {
            texts.add(figmaMapper.toTextDto(node, pageName, node.getFrameName(), currentPath));
        }

        if ("RECTANGLE".equals(node.getType()) && Boolean.TRUE.equals(node.getHasImageFill())) {
            images.add(figmaMapper.toImageDto(node, pageName, node.getFrameName(), currentPath));
        }

        if (node.getChildren() != null) {
            for (FigmaNode child : node.getChildren()) {
                traverseNode(child, pageName, currentPath, texts, images);
            }
        }
    }

    /**
     * Обходит список узлов и собирает тексты и изображения.
     */
    public void traverseNodes(List<FigmaNode> nodes, String pageName, String path,
                              List<FigmaTextDto> texts, List<FigmaImageDto> images) {
        if (nodes == null) return;
        for (FigmaNode node : nodes) {
            traverseNode(node, pageName, path, texts, images);
        }
    }
}
