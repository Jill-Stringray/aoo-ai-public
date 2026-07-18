package com.clockshore.aooai.service;

import com.clockshore.aooai.dto.rerank.RerankRequest;
import com.clockshore.aooai.dto.rerank.RerankResponse;
import com.clockshore.aooai.dto.rerank.RerankResult;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
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
