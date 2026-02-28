package com.company.figmaintegrationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FigmaImageDto {
    private String pageName;
    private String frameName;
    private String nodeName;
    private String nodeId;
    private String imageUrl;
    private String path;
    private String extractedText;
}
