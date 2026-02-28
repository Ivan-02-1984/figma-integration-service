package com.company.figmaintegrationservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final FigmaProperties properties;

    @Bean
    public WebClient figmaWebClient() {

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer ->
                        configurer.defaultCodecs()
                                .maxInMemorySize(100 * 1024 * 1024)
                )
                .build();

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .exchangeStrategies(strategies)
                .build();
    }
}
