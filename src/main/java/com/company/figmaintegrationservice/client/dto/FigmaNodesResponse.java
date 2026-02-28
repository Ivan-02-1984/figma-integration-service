package com.company.figmaintegrationservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * DTO для ответа API Figma при запросе узлов по ID.
 * Структура ответа: { "nodes": { "nodeId": { "document": FigmaNode } } }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FigmaNodesResponse {
    private Map<String, NodeWrapper> nodes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeWrapper {
        private FigmaNode document;
    }
}
