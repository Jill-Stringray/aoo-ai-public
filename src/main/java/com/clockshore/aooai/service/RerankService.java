package com.clockshore.aooai.service;

import com.clockshore.aooai.dto.rerank.RerankRequest;
import com.clockshore.aooai.dto.rerank.RerankResponse;
import com.clockshore.aooai.dto.rerank.RerankResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@Slf4j
public class RerankService {

    private final WebClient webClient;
    private final String apiKey;

    public RerankService(
            @Qualifier("siliconFlow") WebClient webClient,
            @Value("${spring.ai.openai.embedding.api-key}") String apiKey
    ) {
        this.webClient = webClient;
        this.apiKey = apiKey;
    }

    //保留原始排序
    public List<RerankResult> rerank(
            String query,
            List<Document> documents
    ) {
        if (query == null || query.isBlank() || documents == null || documents.isEmpty()) {
            return List.of();
        }

        List<String> documentTexts = documents.stream()
                .map(Document::getText)
                .filter(text -> text != null && !text.isBlank())
                .toList();

        if (documentTexts.isEmpty()) {
            return List.of();
        }

        RerankRequest request = new RerankRequest(
                "BAAI/bge-reranker-v2-m3",
                query,
                documentTexts,
                Math.min(5, documentTexts.size()),
                true
        );


        return webClient.post()
                .uri("/v1/rerank")
                .header(
                        "Authorization",
                        "Bearer " + apiKey
                )
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> {
                                    log.warn("Rerank接口请求失败, status={}, body={}", response.statusCode(), body);
                                    return new IllegalStateException("Rerank接口请求失败: " + response.statusCode());
                                }))
                .bodyToMono(RerankResponse.class)
                .block()
                .results();

    }
}
