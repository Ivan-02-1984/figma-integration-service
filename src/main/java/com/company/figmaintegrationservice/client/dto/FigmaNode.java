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
public class FigmaNode {
    private String id;
    private String name;
    private String type; // "TEXT", "RECTANGLE", etc.
    private String characters; // текст для TEXT
    private String frameName;  // можно заполнять при обходе
    private List<FigmaNode> children;
    private Boolean hasImageFill; // заглушка для RECTANGLE с картинкой
    private String imageUrl;
}
