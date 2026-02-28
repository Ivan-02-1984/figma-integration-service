package com.company.figmaintegrationservice.client;

import com.company.figmaintegrationservice.client.dto.FigmaFileResponse;
import com.company.figmaintegrationservice.client.dto.FigmaImageResponse;
import com.company.figmaintegrationservice.client.dto.FigmaNodesResponse;
import reactor.core.publisher.Mono;

public interface IFigmaClient {
    Mono<FigmaFileResponse> getFile(String token, String fileKey);
    Mono<FigmaImageResponse> getImages(String token, String fileKey, String nodeIds);
    Mono<FigmaNodesResponse> getNodes(String token, String fileKey, String nodeIds, int depth);
}