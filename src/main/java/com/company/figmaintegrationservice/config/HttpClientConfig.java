package com.company.figmaintegrationservice.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    @Value("${figma.archive.connection-pool-size:20}")
    private int poolSize;

    @Value("${figma.archive.timeout-seconds:600}")
    private int timeoutSeconds;

    @Bean
    public PoolingHttpClientConnectionManager poolingConnectionManager() {

        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(Timeout.ofSeconds(timeoutSeconds))
                .build();

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(30))
                .setSocketTimeout(Timeout.ofSeconds(timeoutSeconds))
                .build();

        PoolingHttpClientConnectionManager manager =
                new PoolingHttpClientConnectionManager();

        manager.setMaxTotal(poolSize);
        manager.setDefaultMaxPerRoute(poolSize);
        manager.setDefaultSocketConfig(socketConfig);
        manager.setDefaultConnectionConfig(connectionConfig);

        return manager;
    }

    @Bean
    public CloseableHttpClient httpClient(PoolingHttpClientConnectionManager connectionManager) {
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .evictIdleConnections(Timeout.ofSeconds(60))
                .build();
    }

    @Bean
    public RestTemplate restTemplate(CloseableHttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(factory);
    }
}
