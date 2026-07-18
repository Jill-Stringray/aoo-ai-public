package com.clockshore.aooai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RerankService {

    private final WebClient webClient;

    public RerankService(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://api.siliconflow.cn")
                .build();
    }


    public List<RerankResult> rerank(
            String query,
            List<Document> documents
    ) {

        RerankRequest request = new RerankRequest(
                "BAAI/bge-reranker-v2-m3",
                query,
                documents.stream()
                        .map(Document::getText)
                        .toList(),
                5,
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
                .bodyToMono(RerankResponse.class)
                .block()
                .results();

    }
}
