package com.company.figmaintegrationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FigmaExportDto {
    private String version;
    private List<FigmaTextDto> texts;
    private List<FigmaImageDto> images;
}

