package com.company.figmaintegrationservice.mapper;

import com.company.figmaintegrationservice.client.dto.FigmaNode;
import com.company.figmaintegrationservice.dto.FigmaImageDto;
import com.company.figmaintegrationservice.dto.FigmaTextDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FigmaMapper {

    default FigmaTextDto toTextDto(FigmaNode node, String pageName, String frameName, String path) {
        if (node == null) return null;
        FigmaTextDto dto = new FigmaTextDto();
        dto.setNodeId(node.getId());
        dto.setNodeName(node.getName());
        dto.setPageName(pageName);
        dto.setFrameName(frameName);
        dto.setText(node.getCharacters());
        dto.setPath(path);
        return dto;
    }

    default FigmaImageDto toImageDto(FigmaNode node, String pageName, String frameName, String path) {
        if (node == null) return null;
        FigmaImageDto dto = new FigmaImageDto();
        dto.setNodeId(node.getId());
        dto.setNodeName(node.getName());
        dto.setPageName(pageName);
        dto.setFrameName(frameName);
        dto.setImageUrl(node.getImageUrl());
        dto.setPath(path);
        return dto;
    }
}


