package com.company.figmaintegrationservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageNode {
    private String id;
    private String name;
    private String type; // "PAGE" или "CANVAS"
    private List<FigmaNode> children;
}
