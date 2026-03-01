package com.company.figmaintegrationservice.client;

import com.company.figmaintegrationservice.client.dto.DocumentNode;
import com.company.figmaintegrationservice.client.dto.FigmaFileResponse;
import com.company.figmaintegrationservice.client.dto.FigmaImageResponse;
import com.company.figmaintegrationservice.client.dto.FigmaNode;
import com.company.figmaintegrationservice.client.dto.FigmaNodesResponse;
import com.company.figmaintegrationservice.client.dto.PageNode;
import com.company.figmaintegrationservice.service.IFigmaFileCacheService;
import com.company.figmaintegrationservice.service.IRateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FigmaClient implements IFigmaClient {

    private final WebClient figmaWebClient;
    private final ObjectMapper objectMapper;
    private final IFigmaFileCacheService cacheService;
    private final IRateLimitService rateLimitService;

    private static final int BATCH_SIZE = 3;
    private static final int DEEP_DEPTH = 10;

    /** Количество повторов при 429 и начальная задержка (сек). */
    private static final int RETRY_MAX_ATTEMPTS = 6;
    private static final Duration RETRY_MIN_BACKOFF = Duration.ofSeconds(5);
    private static final Duration RETRY_MAX_BACKOFF = Duration.ofSeconds(120);

    private static boolean is429(Throwable t) {
        if (t instanceof WebClientResponseException e) {
            return e.getStatusCode() != null && e.getStatusCode().value() == 429;
        }
        return t.getMessage() != null && t.getMessage().contains("429");
    }

    @Override
    public Mono<FigmaFileResponse> getFile(String token, String fileKey) {
        return cacheService.getOrCompute(fileKey, () -> getFullFileSmart(token, fileKey));
    }

    /** Пропуск запроса через rate limiter для api.figma.com. */
    private <T> Mono<T> withRateLimit(Mono<T> request) {
        return Mono.fromCallable(() -> {
                    rateLimitService.acquire();
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then(request);
    }

    @Override
    public Mono<FigmaImageResponse> getImages(String token, String fileKey, String nodeIds) {
        return withRateLimit(figmaWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/images/{fileKey}")
                        .queryParam("ids", nodeIds)
                        .queryParam("format", "png")
                        .build(fileKey))
                .headers(h -> h.set("X-Figma-Token", token))
                .retrieve()
                .bodyToMono(FigmaImageResponse.class)
                .delayElement(Duration.ofMillis(500)));
    }

    @Override
    public Mono<FigmaNodesResponse> getNodes(String token, String fileKey, String nodeIds, int depth) {
        return withRateLimit(figmaWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/files/{fileKey}/nodes")
                        .queryParam("ids", nodeIds)
                        .queryParam("depth", depth)
                        .build(fileKey))
                .headers(h -> h.set("X-Figma-Token", token))
                .retrieve()
                .bodyToMono(FigmaNodesResponse.class)
                .retryWhen(Retry.backoff(RETRY_MAX_ATTEMPTS, RETRY_MIN_BACKOFF)
                        .maxBackoff(RETRY_MAX_BACKOFF)
                        .filter(FigmaClient::is429)
                        .doBeforeRetry(s -> log.warn("⚠️ 429 от Figma API, повтор (попытка {})", s.totalRetries() + 1))));
    }

    public Mono<FigmaFileResponse> getFullFileSmart(String token, String fileKey) {
        log.info("📤 Начинаем умную выгрузку файла: {}", fileKey);

        return withRateLimit(getMetadata(token, fileKey))
                .flatMap(metadata -> {
                    List<PageNode> pages = metadata.getDocument().getChildren();
                    if (pages == null || pages.isEmpty()) {
                        return Mono.just(metadata);
                    }

                    log.info("📊 Найдено страниц: {}", pages.size());

                    return Flux.fromIterable(pages)
                            .buffer(BATCH_SIZE)
                            .delayElements(Duration.ofSeconds(2))
                            .concatMap(batch -> withRateLimit(loadPagesBatch(token, fileKey, batch)))
                            .collectList()
                            .map(batchesResults -> {
                                return assembleFullDocument(metadata, batchesResults);
                            });
                })
                .doOnNext(resp -> log.info("✅ Файл полностью выгружен"));
    }

    private Mono<FigmaFileResponse> getMetadata(String token, String fileKey) {
        return figmaWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/files/{fileKey}")
                        .queryParam("depth", 1)
                        .build(fileKey))
                .headers(h -> h.set("X-Figma-Token", token))
                .retrieve()
                .bodyToMono(FigmaFileResponse.class)
                .retryWhen(Retry.backoff(RETRY_MAX_ATTEMPTS, RETRY_MIN_BACKOFF)
                        .maxBackoff(RETRY_MAX_BACKOFF)
                        .filter(FigmaClient::is429)
                        .doBeforeRetry(s -> log.warn("⚠️ 429 от Figma API (metadata), повтор (попытка {})", s.totalRetries() + 1)));
    }

    private Mono<Map<String, FigmaNode>> loadPagesBatch(String token, String fileKey, List<PageNode> pagesBatch) {
        String pageIds = pagesBatch.stream()
                .map(PageNode::getId)
                .collect(Collectors.joining(","));

        log.info("📦 Загружаем пакет из {} страниц: {}", pagesBatch.size(), pageIds);

        return figmaWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/files/{fileKey}/nodes")
                        .queryParam("ids", pageIds)
                        .queryParam("depth", DEEP_DEPTH)
                        .build(fileKey))
                .headers(h -> h.set("X-Figma-Token", token))
                .retrieve()
                .bodyToMono(FigmaNodesResponse.class)
                .map(response -> {
                    // Преобразуем FigmaNodesResponse в Map<String, FigmaNode> для обратной совместимости
                    Map<String, FigmaNode> result = new java.util.HashMap<>();
                    if (response != null && response.getNodes() != null) {
                        for (Map.Entry<String, FigmaNodesResponse.NodeWrapper> entry : response.getNodes().entrySet()) {
                            if (entry.getValue() != null && entry.getValue().getDocument() != null) {
                                result.put(entry.getKey(), entry.getValue().getDocument());
                            }
                        }
                    }
                    return result;
                })
                .retryWhen(Retry.backoff(RETRY_MAX_ATTEMPTS, RETRY_MIN_BACKOFF)
                        .maxBackoff(RETRY_MAX_BACKOFF)
                        .filter(FigmaClient::is429)
                        .doBeforeRetry(s -> log.warn("⚠️ 429 от Figma API (nodes batch), повтор (попытка {})", s.totalRetries() + 1)));
    }

    private FigmaFileResponse assembleFullDocument(FigmaFileResponse metadata,
                                                   List<Map<String, FigmaNode>> batchesResults) {
        FigmaFileResponse fullDocument = new FigmaFileResponse();
        fullDocument.setVersion(metadata.getVersion());

        DocumentNode fullDoc = new DocumentNode();
        List<PageNode> fullPages = new ArrayList<>();

        if (metadata.getDocument() != null && metadata.getDocument().getChildren() != null) {
            for (PageNode page : metadata.getDocument().getChildren()) {
                for (Map<String, FigmaNode> batch : batchesResults) {
                    if (batch.containsKey(page.getId())) {
                        FigmaNode fullNode = batch.get(page.getId());
                        PageNode fullPage = new PageNode();
                        fullPage.setId(fullNode.getId());
                        fullPage.setName(fullNode.getName());
                        fullPage.setType(fullNode.getType());
                        fullPage.setChildren(fullNode.getChildren());
                        fullPages.add(fullPage);
                        break;
                    }
                }
            }
        }

        fullDoc.setChildren(fullPages);
        fullDocument.setDocument(fullDoc);
        return fullDocument;
    }

    public Flux<FigmaImageResponse> getImagesBatch(String token, String fileKey, List<String> nodeIds) {
        if (nodeIds.isEmpty()) {
            return Flux.empty();
        }

        return Flux.fromIterable(nodeIds)
                .buffer(20) // по 20 ID за раз
                .delayElements(Duration.ofSeconds(1))
                .concatMap(batch -> {
                    String ids = String.join(",", batch);
                    return figmaWebClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/images/{fileKey}")
                                    .queryParam("ids", ids)
                                    .queryParam("format", "png")
                                    .build(fileKey))
                            .headers(h -> h.set("X-Figma-Token", token))
                            .retrieve()
                            .bodyToMono(FigmaImageResponse.class)
                            .delayElement(Duration.ofMillis(500));
                });
    }
}