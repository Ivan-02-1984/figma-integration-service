package com.company.figmaintegrationservice.config;

import lombok.Data;

@Data
public class ArchiveSettings {
    private boolean includeRegistry = true;
    private String[] registryFormats = {"csv"};
    private String exportMode = "full";
    private String nodeIds;
    private int nodeDepth = 2;
    private boolean includeText = true;
    private boolean includeImages = true;
    private boolean includeFrames = false;
    private boolean includeGroups = false;

    public boolean hasFormat(String format) {
        if (registryFormats == null) return false;
        for (String f : registryFormats) {
            if (f.trim().equalsIgnoreCase(format)) return true;
        }
        return false;
    }

    public boolean isSelectedMode() {
        return "selected".equals(exportMode) && nodeIds != null && !nodeIds.isEmpty();
    }
}