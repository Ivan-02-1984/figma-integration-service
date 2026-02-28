package com.company.figmaintegrationservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "figma")
@Getter
@Setter
public class FigmaProperties {
    private String baseUrl;
}
